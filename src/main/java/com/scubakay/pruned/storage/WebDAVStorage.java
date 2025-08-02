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
        try {
            sardine.createDirectory(folderUri.toString());
            if (Config.debug) PrunedMod.LOGGER.error("Created parent folder for {}", relativePath);
        } catch (IOException e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to create parent folder for {}: {}", relativePath, e.getMessage());
        }
    }

    private URI getOrCreatePrunedFolder() {
        String endpoint = Config.webDavEndpoint;
        if (!endpoint.endsWith("/")) endpoint += "/";
        endpoint += "Pruned/";
        URI uri = URI.create(endpoint);

        try {
            sardine.createDirectory(uri.toString());
            if (Config.debug) PrunedMod.LOGGER.error("Created pruned folder");
        } catch (IOException e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to create pruned folder: {}", e.getMessage());
        }

        return uri;
    }
}
