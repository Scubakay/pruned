package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
        try {
            URL prunedFolder = getOrCreatePrunedFolder();
            createParentRecursive(prunedFolder, relativePath);
            URL fileUri = getFileUri(prunedFolder, relativePath);

            byte[] fileBytes = java.nio.file.Files.readAllBytes(filepath.normalize());
            sardine.put(fileUri.toString(), fileBytes);
            if (Config.debug) PrunedMod.LOGGER.info("Uploaded {} to {}", filepath.getFileName(), relativePath);
        } catch (MalformedURLException e) {
            if (Config.debug) PrunedMod.LOGGER.error("Could not form URL: {}", e.getMessage());
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to upload {} to {}: {}", filepath.getFileName(), relativePath, e.getMessage());
        }
    }

    private URL getFileUri(URL prunedFolder, Path relativePath) throws MalformedURLException {
        // Encode each segment of the relative path for URL path (spaces as %20, not +)
        StringBuilder encodedPath = new StringBuilder();
        for (Path segment : relativePath) {
            String s = segment.toString();
            // Replace spaces with %20 and leave other characters as-is
            s = s.replace(" ", "%20");
            encodedPath.append(s).append("/");
        }
        // Remove trailing slash if not a directory
        if (!encodedPath.isEmpty() && !relativePath.toString().endsWith("/")) {
            encodedPath.setLength(encodedPath.length() - 1);
        }
        try {
            return prunedFolder.toURI().resolve(encodedPath.toString()).toURL();
        } catch (Exception e) {
            throw new MalformedURLException("Failed to resolve URL: " + e.getMessage());
        }
    }

    private void createParentRecursive(URL baseUrl, Path relativePath) {
        Path parent = relativePath.getParent();
        if (parent == null) return;

        try {
            createParentRecursive(baseUrl, parent);
            URL folderUri = getFileUri(baseUrl, parent);
            getOrCreateFolder(folderUri);
        } catch (MalformedURLException e) {
            if (Config.debug) PrunedMod.LOGGER.info("Malformed URL: {} + {}; {}", baseUrl, relativePath, e.getMessage());
        }
    }

    private URL getOrCreatePrunedFolder() throws MalformedURLException {
        return getOrCreateFolder(getPrunedUri());
    }

    private URL getOrCreateFolder(URL uri) {
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

    private static URL getPrunedUri() throws MalformedURLException {
        String endpoint = Config.webDavEndpoint;
        if (!endpoint.endsWith("/")) endpoint += "/";
        endpoint += "Pruned/";
        return URI.create(endpoint).toURL();
    }
}
