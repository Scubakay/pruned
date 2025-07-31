package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;

import java.io.FileInputStream;
import java.io.IOException;
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

    public void uploadWorldFile(String worldName, Path localPath, Path relativePath) {
        String normalizedRelativePath = relativePath.normalize().toString().replace("\\", "/");
        String endpoint = Config.webDavEndpoint;
        if (!endpoint.endsWith("/")) endpoint += "/";
        String baseRemotePath = endpoint + "Pruned/" + worldName + "/";
        String remotePath = baseRemotePath + normalizedRelativePath;
        try {
            createWorldFolder(worldName, endpoint);
            createParentDirectories(normalizedRelativePath, baseRemotePath);
            sardine.put(remotePath, new FileInputStream(localPath.normalize().toFile()));
            if (Config.debug) PrunedMod.LOGGER.info("Uploaded {} to {}", localPath.getFileName(), remotePath);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to upload {} to {}: {}", localPath, remotePath, e.getMessage());
        }
    }

    private void createWorldFolder(String worldName, String endpoint) {
        final String prunedFolder = endpoint + "Pruned/";
        final String worldFolder = prunedFolder + worldName + "/";

        try {
            if (!sardine.exists(worldFolder)) {
                if (!sardine.exists(prunedFolder)) {
                    sardine.createDirectory(prunedFolder);
                }
                sardine.createDirectory(worldFolder);
            }
        } catch (IOException e) {
            if (Config.debug) PrunedMod.LOGGER.error("Something went wrong trying to create remote world directory: {}", e.getMessage());
        }
    }

    private void createParentDirectories(String normalizedRelativePath, String baseRemotePath) throws IOException {
        String[] segments = normalizedRelativePath.split("/");
        StringBuilder currentPath = new StringBuilder(baseRemotePath);
        for (int i = 0; i < segments.length - 1; i++) {
            currentPath.append(segments[i]).append("/");
            String folderPath = currentPath.toString();
            try {
                sardine.list(folderPath); // Uses GET, sends credentials
            } catch (IOException e) {
                sardine.createDirectory(folderPath);
            }
        }
    }
}
