package com.scubakay.pruned.storage.webdav.task;

import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.storage.webdav.WebDAVStorage;
import com.scubakay.pruned.util.FileHasher;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class UploadFileTask implements Runnable {
    private final MinecraftServer server;
    private final Path path;

    public UploadFileTask(MinecraftServer server, Path path) {
        this.server = server;
        this.path = path;
    }

    @Override
    public void run() {
        if (!WebDAVStorage.isConnected()) {
            PrunedMod.LOGGER.error("Uploading file failed: Not connected to WebDAV server");
            return;
        }

        String sha1 = PrunedData.getServerState(server).getSha1(path);
        String newSha1 = FileHasher.getSha1(path);
        if (newSha1.equals(sha1)) {
            if (Config.debug) PrunedMod.LOGGER.info("Skipping up to date file: {}", path);
            return;
        }

        try {
            WebDAVStorage.getInstance().uploadFile(path);
            PrunedData.getServerState(server).updateSha1(path, newSha1);
            if (Config.debug) PrunedMod.LOGGER.info("Uploaded {}", path);
        } catch (Exception e) {
            PrunedMod.LOGGER.error(e.getMessage());
        }
    }
}
