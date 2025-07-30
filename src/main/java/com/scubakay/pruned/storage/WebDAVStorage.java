package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WebDAVStorage {
    private static WebDAVStorage instance;
    private Sardine sardine;

    private WebDAVStorage() {
        sardine = SardineFactory.begin(Config.webDavUsername, Config.webDavPassword);
    }

    public static WebDAVStorage getInstance() {
        if (instance == null) {
            instance = new WebDAVStorage();
        }
        return instance;
    }

    public void uploadWorldFile(String worldName, String localPath, String relativePath) {
        try {
            String baseFolder = Config.webDavEndpoint;
            if (!baseFolder.endsWith("/")) baseFolder += "/";
            baseFolder += "Pruned/";
            String encodedWorldName = URLEncoder.encode(worldName, StandardCharsets.UTF_8);
            baseFolder += encodedWorldName + "/";
            // Split and encode each segment of relativePath
            String[] folders = relativePath.replace("\\", "/").split("/");
            String currentFolder = baseFolder;
            for (int i = 0; i < folders.length - 1; i++) {
                String encodedFolder = URLEncoder.encode(folders[i], StandardCharsets.UTF_8);
                currentFolder += encodedFolder + "/";
                if (sardine.exists(currentFolder)) {
                    // If exists, check if it's a directory
                    boolean isDir = false;
                    try {
                        isDir = sardine.list(currentFolder).stream().anyMatch(d -> d.isDirectory());
                    } catch (Exception ignored) {}
                    if (!isDir) {
                        if (Config.debug) PrunedMod.LOGGER.error("Conflict: {} exists and is not a directory", currentFolder);
                        return;
                    }
                } else {
                    sardine.createDirectory(currentFolder);
                }
            }
            // Encode the file name
            String encodedFileName = URLEncoder.encode(folders[folders.length - 1], StandardCharsets.UTF_8);
            String targetPath = currentFolder + encodedFileName;
            // If targetPath exists and is a directory, log error
            if (sardine.exists(targetPath)) {
                boolean isDir = false;
                try {
                    isDir = sardine.list(targetPath).stream().anyMatch(d -> d.isDirectory());
                } catch (Exception ignored) {}
                if (isDir) {
                    if (Config.debug) PrunedMod.LOGGER.error("Conflict: {} exists and is a directory, cannot overwrite with file", targetPath);
                    return;
                }
            }
            sardine.put(targetPath, new java.io.FileInputStream(localPath));
            if (Config.debug) PrunedMod.LOGGER.info("Uploaded {} to {}", localPath, targetPath);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to upload {}: {}", localPath, e.getMessage());
        }
    }
}
