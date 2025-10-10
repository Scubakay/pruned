package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.exception.CreateFolderException;
import com.scubakay.pruned.exception.RemoveException;
import com.scubakay.pruned.exception.UploadException;
import com.scubakay.pruned.util.MachineIdentifier;
import com.scubakay.pruned.util.PasswordEncryptor;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WebDAVStorage {
    private static WebDAVStorage instance;
    private final Sardine sardine;
    private final List<URL> existingFolders;

    private WebDAVStorage() {
        String machineId = MachineIdentifier.getMachineId();
        String decryptedPassword = PasswordEncryptor.decrypt(Config.webDavPassword, machineId);
        sardine = SardineFactory.begin(Config.webDavUsername, decryptedPassword);
        sardine.enablePreemptiveAuthentication(URI.create(Config.webDavEndpoint).getHost());
        existingFolders = new ArrayList<>();
    }

    public static WebDAVStorage getInstance() {
        return instance;
    }

    public static boolean isConnected() {
        return instance != null;
    }

    public static void connect(MinecraftServer ignoredServer) {
        PrunedMod.LOGGER.info("Connecting to WebDAV server...");
        instance = new WebDAVStorage();
        try {
            instance.getOrCreateFolder(getEndpoint());
            PrunedMod.LOGGER.info("Connected to WebDAV server");
        } catch (Exception e) {
            instance = null;
            PrunedMod.LOGGER.error("Could not connect to WebDAV server: {}", e.getMessage());
        }

    }

    public static void disconnect(MinecraftServer ignoredServer) {
        // Should probably cancel all running uploads or something.
    }

    public void uploadFile(Path filepath, Path relativePath) throws UploadException {
        URL fileURL = null;
        String mimeType = "";
        try {
            mimeType = Files.probeContentType(filepath);
            if (mimeType == null) mimeType = "application/octet-stream";
            byte[] fileBytes = Files.readAllBytes(filepath.normalize());

            sardine.put(resolveHostUrl(relativePath).toString(), fileBytes);
        } catch (CreateFolderException e) {
            throw new UploadException(String.format("Could not form URL: %s", e.getMessage()));
        } catch (IOException e) {
            throw new UploadException(String.format("Failed to upload %s file %s to %s: %s", mimeType, filepath.getFileName(), fileURL, e.getMessage()));
        }
    }

    public void removeWorldFile(Path relativePath) throws RemoveException {
        try {
            sardine.delete(resolveHostUrl(relativePath).toString());
        } catch (CreateFolderException e) {
            throw new RemoveException(String.format("Could not form URL: %s", e.getMessage()));
        } catch (Exception e) {
            throw new RemoveException(String.format("Failed to remove file %s", e.getMessage()));
        }
    }

    private URL getUploadFolder(URL baseUrl, Path relativePath) throws CreateFolderException {
        Path parent = relativePath.getParent();
        if (parent == null) return baseUrl;
        getUploadFolder(baseUrl, parent);
        return getOrCreateFolder(resolveHostUrl(parent));
    }

    private URL getWorldSaveFolder(String worldName) throws MalformedURLException, URISyntaxException {
        return getOrCreateFolder(getWorldSaveURL(worldName));
    }

    private URL resolveHostUrl(Path relativePath) throws CreateFolderException {
        try {
            URL prunedFolder = getWorldSaveFolder(relativePath.getName(0).toString());
            URL uploadFolder = getUploadFolder(prunedFolder, relativePath);
            String uriPath = relativePath.toString().replace("\\", "/");
            return uploadFolder.toURI().resolve(uriPath).normalize().toURL();
        } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            throw new CreateFolderException(e.getMessage());
        }
    }

    private URL getOrCreateFolder(URL url) {
        try {
            if (this.existingFolders.contains(url)) {
                return url;
            }
            if (!sardine.exists(url.toString())) {
                sardine.createDirectory(url.toString());
                if (Config.debug) PrunedMod.LOGGER.info("Created folder {}", url);
                this.existingFolders.add(url);
            }
        } catch (IOException e) {
            //noinspection StatementWithEmptyBody
            if (e.getMessage() != null && e.getMessage().contains("405")) {
                // 405 Method Not Allowed: treat as folder already exists
                //if (Config.debug) PrunedMod.LOGGER.info("Folder already exists (405) at: {}", url);
            } else {
                if (Config.debug) PrunedMod.LOGGER.error("Failed to create folder {}: {}", url, e.getMessage());
            }
        }
        return url;
    }

    private static URL getWorldSaveURL(String worldName) throws MalformedURLException {
        URI endpointUri = URI.create(Config.webDavEndpoint);
        URI worldUri = endpointUri.resolve("Pruned/").resolve(worldName + "/");
        return worldUri.toURL();
    }

    private static URL getEndpoint() throws MalformedURLException {
        return URI.create(Config.webDavEndpoint)
                .resolve("Pruned")
                .toURL();
    }
}
