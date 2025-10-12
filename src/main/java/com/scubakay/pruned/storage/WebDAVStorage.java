package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.exception.CreateFolderException;
import com.scubakay.pruned.exception.RemoveException;
import com.scubakay.pruned.exception.UploadException;
import com.scubakay.pruned.util.MachineIdentifier;
import com.scubakay.pruned.util.PasswordEncryptor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WebDAVStorage {
    private static WebDAVStorage instance;
    private final Sardine sardine;

    final URI webDavEndpoint;
    private final URI worldSaveURL;

    private static final ConcurrentMap<URI, Boolean> createdFolders = new ConcurrentHashMap<>();

    private WebDAVStorage(MinecraftServer server) {
        webDavEndpoint = URI.create(Config.webDavEndpoint);
        worldSaveURL = getWorldSaveUri(server);

        String machineId = MachineIdentifier.getMachineId();
        String decryptedPassword = PasswordEncryptor.decrypt(Config.webDavPassword, machineId);
        sardine = SardineFactory.begin(Config.webDavUsername, decryptedPassword);
        sardine.enablePreemptiveAuthentication(webDavEndpoint.getHost());
    }

    //region Initialization

    public static WebDAVStorage getInstance() {
        return instance;
    }

    public static boolean isConnected() {
        return instance != null;
    }

    public static void connect(MinecraftServer server) {
        if (!PrunedData.getServerState(server).isActive()) {
            if (Config.debug) PrunedMod.LOGGER.info("Skipping WebDAV connection: Pruned is not active in this world.");
            return;
        }

        try {
            PrunedMod.LOGGER.info("Connecting to WebDAV server...");
            instance = new WebDAVStorage(server);
            instance.getOrCreateFolder(instance.worldSaveURL);
            PrunedMod.LOGGER.info("Connected to WebDAV server");
        } catch (Exception e) {
            instance = null;
            PrunedMod.LOGGER.error(e.getMessage());
            PrunedMod.LOGGER.error("Could not connect to WebDAV server!");
        }
    }

    public static void disconnect(MinecraftServer ignoredServer) {
        // maybe shut down sardine?
    }

    //endregion
    //region WebDAV calls

    public void uploadFile(Path filepath, Path relativePath) throws UploadException, CreateFolderException {
        URI hostUrl = null;
        try {
            hostUrl = resolveHostUrl(relativePath);

            final URI parentUri = getParentUri(hostUrl);
            if (parentUri != null) {
                getOrCreateFolder(parentUri);
            }

            byte[] fileBytes = Files.readAllBytes(filepath.normalize());
            sardine.put(hostUrl.toString(), fileBytes);
        } catch (IOException e) {
            throw new UploadException(String.format("Failed to upload %s to %s: %s", relativePath, hostUrl, e.getMessage()));
        }
    }

    public void removeWorldFile(Path relativePath) throws RemoveException {
        URI hostUrl = null;
        try {
            hostUrl = resolveHostUrl(relativePath);
            sardine.delete(hostUrl.toString());
        } catch (Exception e) {
            throw new RemoveException(String.format("Failed to remove %s from %s: %s", relativePath, hostUrl, e.getMessage()));
        }
    }

    private void getOrCreateFolder(URI uri) throws CreateFolderException {
        if (uri.equals(webDavEndpoint) || createdFolders.containsKey(uri)) {
            createdFolders.put(uri, true);
            return;
        }
        URI parent = getParentUri(uri);
        if (parent != null) {
            getOrCreateFolder(parent);
        }
        synchronized (createdFolders) {
            createFolder(uri);
            createdFolders.put(uri, true);
        }
    }

    private void createFolder(URI uri) throws CreateFolderException {
        if (webDavEndpoint.toString().contains(uri.toString())) {
            throw new CreateFolderException(String.format("Tried to create folder within WebDAV endpoint %s", uri));
        }
        try {
            if (!folderExists(uri)) {
                sardine.createDirectory(uri.toString());
                if (Config.debug) PrunedMod.LOGGER.info("Created folder {}", uri);
            }
        } catch (IOException e) {
            String message = e.getMessage();
            if (message.contains("409")) {
                throw new CreateFolderException(String.format("Folder %s already exists (409): %s", uri, message));
            } else if (message.contains("400")) {
                throw new CreateFolderException(String.format("Folder %s bad request (400): %s", uri, message));
            } else if (message.contains("404")) {
                throw new CreateFolderException(String.format("Parent folder not found for %s: %s", uri, message));
            } else if (message.contains("405")) {
                throw new CreateFolderException(String.format("Folder %s method not allowed (405): %s", uri, message));
            } else {
                throw new CreateFolderException(String.format("Failed to create folder %s: %s", uri, message));
            }
        }
    }

    private boolean folderExists(URI uri) throws CreateFolderException {
        if (uri.equals(webDavEndpoint)) return true; // Assume endpoint always exists
        final boolean exists;
        try {
            exists = sardine.exists(uri.toString());
        } catch (IOException e) {
            throw new CreateFolderException(String.format("Could not determine if %s exists: %s", uri, e.getMessage()));
        }
        return exists;
    }
    //endregion
    //region URI building

    private URI resolveHostUrl(Path relativePath) throws IllegalArgumentException {
        String uriPath = relativePath.toString().replace("\\", "/");
        return worldSaveURL.resolve(uriPath);
    }

    private URI getParentUri(URI uri) {
        if (uri.equals(webDavEndpoint)) return null; // Don't go above endpoint
        String path = uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return null;
        String parentPath = path.substring(0, lastSlash);
        URI parent = URI.create(uri.getScheme() + "://" + uri.getHost() + parentPath);
        // If parent is the same as endpoint or part of it, return null
        if (webDavEndpoint.toString().contains(parent.toString())) return null;
        return parent;
    }

    private @NotNull URI getWorldSaveUri(MinecraftServer server) {
        return webDavEndpoint
                .resolve(Config.uploadFolder + "/")
                .resolve(getWorldName(server) + "/");
    }

    private @NotNull String getWorldName(MinecraftServer server) {
        return getWorldSavePath(server).getParent().getFileName().toString();
    }

    private Path getWorldSavePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT);
    }

    //endregion
}
