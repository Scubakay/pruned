package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
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
        URL fileUri = null;
        String mimeType = "";
        try {
            URL prunedFolder = getOrCreatePrunedFolder();
            createParentRecursive(prunedFolder, relativePath);

            fileUri = getUrl(prunedFolder, relativePath);
            mimeType = Files.probeContentType(filepath);
            if (mimeType == null) mimeType = "application/octet-stream";
            byte[] fileBytes = Files.readAllBytes(filepath.normalize());

            sardine.put(fileUri.toString(), fileBytes, mimeType);
            if (Config.debug) PrunedMod.LOGGER.info("Uploaded: {}", relativePath);
        } catch (MalformedURLException | URISyntaxException e) {
            if (Config.debug) PrunedMod.LOGGER.error("Could not form URL: {}", e.getMessage());
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to upload {} file {} to {}: {}", mimeType, filepath.getFileName(), fileUri, e.getMessage());
        }
    }

    public void removeWorldFile(Path relativePath) {
        try {
            URL prunedFolder = getOrCreatePrunedFolder();
            URL fileUri = getUrl(prunedFolder, relativePath);
            sardine.delete(fileUri.toString());
            if (Config.debug) PrunedMod.LOGGER.info("Removed: {}", relativePath);
        } catch (MalformedURLException | URISyntaxException e) {
            if (Config.debug) PrunedMod.LOGGER.error("Could not form URL: {}", e.getMessage());
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to remove file {}", e.getMessage());
        }
    }

    private void createParentRecursive(URL baseUrl, Path relativePath) {
        Path parent = relativePath.getParent();
        if (parent == null) return;
        try {
            createParentRecursive(baseUrl, parent);
            URL folderUri = getUrl(baseUrl, parent);
            getOrCreateFolder(folderUri);
        } catch (MalformedURLException | URISyntaxException e) {
            if (Config.debug) PrunedMod.LOGGER.info("Malformed URL: {} + {}; {}", baseUrl, relativePath, e.getMessage());
        }
    }

    private URL getOrCreatePrunedFolder() throws MalformedURLException {
        return getOrCreateFolder(getPrunedUri());
    }

    private URL getUrl(URL prunedFolder, Path relativePath) throws MalformedURLException, URISyntaxException {
        StringBuilder encodedPath = new StringBuilder();
        for (Path segment : relativePath) {
            String s = segment.toString().replace(" ", "%20");
            if (!encodedPath.isEmpty()) encodedPath.append("/");
            encodedPath.append(s);
        }
        return prunedFolder.toURI().resolve(encodedPath.toString()).toURL();
    }

    private URL getOrCreateFolder(URL url) {
        try {
            if (!sardine.exists(url.toString())) {
                sardine.createDirectory(url.toString());
                if (Config.debug) PrunedMod.LOGGER.info("Created folder {}", url);
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

    private static URL getPrunedUri() throws MalformedURLException {
        String endpoint = Config.webDavEndpoint;
        if (!endpoint.endsWith("/")) endpoint += "/";
        endpoint += "Pruned/";
        return URI.create(endpoint).toURL();
    }
}
