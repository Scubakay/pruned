package com.scubakay.pruned.storage.webdav.task;

import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.storage.webdav.WebDAVStorage;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public class RemoveFileTask implements Runnable {
    private final MinecraftServer server;
    private final Path path;

    public RemoveFileTask(MinecraftServer server, Path path) {
        this.server = server;
        this.path = path;
    }

    @Override
    public void run() {
        if (Config.debug) PrunedMod.LOGGER.info("RemoveFileTask started for {}", path);
        if (!WebDAVStorage.isConnected()) {
            PrunedMod.LOGGER.error("Removing file failed: Not connected to WebDAV server");
            return;
        }

        try {
            WebDAVStorage.getInstance().removeWorldFile(path);
            PrunedData.getServerState(server).removeFile(path);
            if (Config.debug) PrunedMod.LOGGER.info("Removed {}", path);
        } catch (Exception e) {
            PrunedMod.LOGGER.error("Removing {} failed: {}", path, e.getMessage());
        }
    }
}
