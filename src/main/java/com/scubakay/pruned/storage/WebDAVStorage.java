package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.exception.RemoveException;
import com.scubakay.pruned.exception.UploadException;
import com.scubakay.pruned.util.MachineIdentifier;
import com.scubakay.pruned.util.PasswordEncryptor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WebDAVStorage {
    private static WebDAVStorage instance;
    private final Sardine sardine;

    private final URI worldSaveURL;

    private static final ConcurrentMap<URI, Boolean> createdFolders = new ConcurrentHashMap<>();

    private WebDAVStorage(MinecraftServer server) {
        String machineId = MachineIdentifier.getMachineId();
        String decryptedPassword = PasswordEncryptor.decrypt(Config.webDavPassword, machineId);
        sardine = SardineFactory.begin(Config.webDavUsername, decryptedPassword);
        sardine.enablePreemptiveAuthentication(URI.create(Config.webDavEndpoint).getHost());

        Path worldSavePath = server.getSavePath(WorldSavePath.ROOT);
        String worldName = worldSavePath.getParent().getFileName().toString();
        worldSaveURL = URI.create(Config.webDavEndpoint)
                .resolve(Config.uploadFolder + "/")
                .resolve(worldName + "/");
    }

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
            PrunedMod.LOGGER.error("Could not connect to WebDAV server: {}", e.getMessage());
        }
    }

    public static void disconnect(MinecraftServer ignoredServer) {
        // maybe shut down sardine?
    }

    public void uploadFile(Path filepath, Path relativePath) throws UploadException {
        URI hostUrl = null;
        try {
            hostUrl = resolveHostUrl(relativePath);
            getOrCreateFolder(hostUrl);

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

    private URI resolveHostUrl(Path relativePath) throws IllegalArgumentException {
        String uriPath = relativePath.toString().replace("\\", "/");
        return worldSaveURL.resolve(uriPath);
    }

    private void getOrCreateFolder(URI uri) {
        while (true) {
            URI parent = getParentUri(uri);
            if (parent.equals(worldSaveURL) || createdFolders.containsKey(parent)) {
                createdFolders.put(parent, true);
                break;
            }
            synchronized (createdFolders) {
                if (!createdFolders.containsKey(parent)) {
                    createFolder(parent);
                    createdFolders.put(parent, true);
                }
            }
        }
    }

    private void createFolder(URI uri) {
        try {
            if (!sardine.exists(uri.toString())) {
                sardine.createDirectory(uri.toString());
                if (Config.debug) PrunedMod.LOGGER.info("Created folder {}", uri);
            }
        } catch (IOException e) {
            String message = e.getMessage();
            if (message.contains("409")) {
                if (Config.debug) PrunedMod.LOGGER.error("Folder {} already exists (409 Conflict): {}", uri, message);
            } else if (message.contains("400")) {
                if (Config.debug) PrunedMod.LOGGER.error("Folder {} bad request (400 Bad Request): {}", uri, message);
            } else if (message.contains("404")) {
                if (Config.debug) PrunedMod.LOGGER.error("Folder {} not found (404 Not Found): {}", uri, message);
            } else if (message.contains("405")) {
                if (Config.debug) PrunedMod.LOGGER.error("Folder {} method not allowed (405 Method Not Allowed): {}", uri, message);
            } else {
                if (Config.debug) PrunedMod.LOGGER.error("Failed to create folder {}: {}", uri, message);
            }
        }
    }

    private URI getParentUri(URI uri) {
        String path = uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) return uri; // Already at root
        String parentPath = path.substring(0, lastSlash);
        return URI.create(uri.getScheme() + "://" + uri.getHost() + parentPath);
    }
}
