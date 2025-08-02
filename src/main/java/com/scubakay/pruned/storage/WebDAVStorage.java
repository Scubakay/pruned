package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

public class WebDAVStorage {
    private static WebDAVStorage instance;
    private final Sardine sardine;

    private WebDAVStorage() {
        sardine = SardineFactory.begin(Config.webDavUsername, Config.webDavPassword);
    }

    public static WebDAVStorage getInstance() {
        if (instance == null) {
            instance = new WebDAVStorage();
        }
        return instance;
    }

    public void uploadWorldFile(Path filepath, Path relativePath) {
        URI prunedFolder = getOrCreatePrunedFolder();
        createParentRecursive(prunedFolder, relativePath);
        URI fileUri = getFileUri(prunedFolder, relativePath);
        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(filepath.normalize());
            sardine.put(fileUri.toString(), fileBytes);
            if (Config.debug) PrunedMod.LOGGER.info("Uploaded {} to {}", filepath.getFileName(), relativePath);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to upload {} to {}: {}", filepath.getFileName(), relativePath, e.getMessage());
        }
    }

    private URI getFileUri(URI prunedFolder, Path relativePath) {
        return prunedFolder.resolve(relativePath.toString().replace("\\", "/"));
    }

    private void createParentRecursive(URI baseUri, Path relativePath) {
        Path parent = relativePath.getParent();
        if (parent == null) return;

        createParentRecursive(baseUri, parent);
        URI folderUri = baseUri.resolve(parent.toString().replace("\\", "/") + "/");
        getOrCreateFolder(folderUri);
    }

    private URI getOrCreatePrunedFolder() {
        return getOrCreateFolder(getPrunedUri());
    }

    private URI getOrCreateFolder(URI uri) {
        try {
            sardine.createDirectory(uri.toString());
            if (Config.debug) PrunedMod.LOGGER.info("Created pruned folder");
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("405")) {
                // 405 Method Not Allowed: treat as folder already exists
                if (Config.debug) PrunedMod.LOGGER.info("Folder already exists (405) at: {}", uri);
            } else {
                if (Config.debug) PrunedMod.LOGGER.error("Failed to create pruned folder: {}", e.getMessage());
            }
        }
        return uri;
    }

    private static URI getPrunedUri() {
        String endpoint = Config.webDavEndpoint;
        if (!endpoint.endsWith("/")) endpoint += "/";
        endpoint += "Pruned/";
        return URI.create(endpoint);
    }
}
