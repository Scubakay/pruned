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

public class WebDAVStorage {
    private static WebDAVStorage instance;
    private final Sardine sardine;

    private final Path worldSavePath;
    private final String worldName;
    private final URI worldSaveURL;

    private WebDAVStorage(MinecraftServer server) {
        String machineId = MachineIdentifier.getMachineId();
        String decryptedPassword = PasswordEncryptor.decrypt(Config.webDavPassword, machineId);
        sardine = SardineFactory.begin(Config.webDavUsername, decryptedPassword);
        sardine.enablePreemptiveAuthentication(URI.create(Config.webDavEndpoint).getHost());

        worldSavePath = server.getSavePath(WorldSavePath.ROOT);
        worldName = worldSavePath.getParent().getFileName().toString();
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
        URI parent = uri; // get parent
        try {
            while (true) {
                parent = getParentUri(parent);
                if (parent.equals(worldSaveURL) || sardine.exists(parent.toString())) {
                    break;
                }
                sardine.createDirectory(parent.toString());
                if (Config.debug) PrunedMod.LOGGER.info("Created folder {}", parent);
            }
        } catch (IOException e) {
            if (e.getMessage() == null || !e.getMessage().contains("405")) {
                if (Config.debug) PrunedMod.LOGGER.error("Failed to create folder {}: {}", parent, e.getMessage());
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
