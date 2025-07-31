package com.scubakay.pruned.storage;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;

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
        try {
            String normalizedRelativePath = relativePath.toString().replace("\\", "/");
            String remotePath = Config.webDavEndpoint + worldName + "/" + normalizedRelativePath;
            sardine.put(remotePath, new java.io.FileInputStream(localPath.toFile()));
            if (Config.debug) PrunedMod.LOGGER.info("Uploaded {} to {}", localPath.getFileName(), normalizedRelativePath);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to upload {} to {}: {}", localPath, relativePath, e.getMessage());
        }
    }
}
