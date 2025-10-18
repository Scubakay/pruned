package com.scubakay.pruned.storage.webdav;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WebDAVStorage {
    private static WebDAVStorage instance;
    private final Sardine sardine;

    final URI webDavEndpoint;
    private final URI worldSaveURL;
    private final Path worldSavePath;

    private final ConcurrentMap<URI, Boolean> createdFolders;


    private WebDAVStorage(MinecraftServer server) {
        createdFolders = new ConcurrentHashMap<>();
        webDavEndpoint = URI.create(Config.webDavEndpoint);
        worldSaveURL = getWorldSaveUri(server);
        worldSavePath = server.getSavePath(WorldSavePath.ROOT);

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

    public void uploadFile(Path filepath) throws UploadException, CreateFolderException {
        URI hostUrl = null;
        try {
            hostUrl = resolveHostUrl(filepath);

            final URI parentUri = getParentUri(hostUrl);
            if (!sardine.exists(hostUrl.toASCIIString()) && parentUri != null) {
                getOrCreateFolder(parentUri);
            }

            byte[] fileBytes = Files.readAllBytes(filepath.normalize());
            sardine.put(hostUrl.toASCIIString(), fileBytes);
        } catch (IOException e) {
            throw new UploadException(String.format("Failed to upload %s: %s", hostUrl, e.getMessage()));
        }
    }

    public void removeWorldFile(Path path) throws RemoveException {
        URI hostUrl = null;
        try {
            hostUrl = resolveHostUrl(path);
            sardine.delete(hostUrl.toASCIIString());
        } catch (Exception e) {
            throw new RemoveException(String.format("Failed to remove %s from %s: %s", path, hostUrl, e.getMessage()));
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
            sardine.createDirectory(uri.toASCIIString());
            if (Config.debug) PrunedMod.LOGGER.info("Created folder {}", uri);
        } catch (IOException e) {
            String message = e.getMessage();
            if (message.contains("409")) {
                throw new CreateFolderException(String.format("Folder %s already exists (409): %s", uri, message));
            } else if (message.contains("400")) {
                throw new CreateFolderException(String.format("Folder %s bad request (400): %s", uri, message));
            } else if (message.contains("404")) {
                throw new CreateFolderException(String.format("Parent folder not found for %s: %s", uri, message));
            } else if (message.contains("405")) {
                if (Config.debug) PrunedMod.LOGGER.info("Folder already exists: {}", uri);
            } else {
                throw new CreateFolderException(String.format("Failed to create folder %s: %s", uri, message));
            }
        }
    }
    //endregion
    //region URI building

    private URI resolveHostUrl(Path path) throws IllegalArgumentException {
        Path relativePath = worldSavePath.getParent().getParent().relativize(path);

        StringBuilder sb = new StringBuilder();
        for (Path segment : relativePath) {
            if (!sb.isEmpty()) sb.append('/');
            sb.append(encode(segment.toString()));
        }
        String uriPath = sb.toString();

        return worldSaveURL.resolve(uriPath);
    }

    private URI getParentUri(URI uri) {
        if (uri.equals(webDavEndpoint)) return null; // Don't go above endpoint

        String ascii = uri.toASCIIString();
        String withSlash = ascii.endsWith("/") ? ascii : ascii + "/";

        // Resolve '..' against a guaranteed directory-like URI and normalize to collapse the '..'
        URI parent = URI.create(withSlash).resolve("..").normalize();

        // If parent is at-or-above the endpoint, return null
        if (webDavEndpoint.toASCIIString().contains(parent.toASCIIString())) return null;

        return parent;
    }

    private @NotNull URI getWorldSaveUri(MinecraftServer server) {
        final String worldName = getWorldName(server);
        return webDavEndpoint
                .resolve(encode(Config.uploadFolder) + "/")
                .resolve(encode(worldName) + "/")
                .resolve(encode(worldName));
    }

    private @NotNull String getWorldName(MinecraftServer server) {
        return getWorldSavePath(server).getParent().getFileName().toString();
    }

    private Path getWorldSavePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    //endregion
}
