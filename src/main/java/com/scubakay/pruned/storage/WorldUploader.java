package com.scubakay.pruned.storage;

import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.util.FileHasher;
import com.scubakay.pruned.util.IgnoreList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldUploader {
    // Single-threaded executor for uploads (can be changed to multithreaded if needed)
    private static final ExecutorService uploadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final ExecutorService removeExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void shutdown(MinecraftServer ignoredServer) {
        uploadExecutor.shutdown();
        removeExecutor.shutdown();
    }

    public static void upload(MinecraftServer server) {
        if (!PrunedData.getServerState(server).isActive()) {
            if (Config.debug) PrunedMod.LOGGER.info("Can't upload: Pruned is not active on this world.");
            return;
        }
        addNonRegionFiles(server, server.getSavePath(WorldSavePath.ROOT));
        PrunedData.getServerState(server).getFiles().forEach((filePath, sha1) -> uploadFile(server, filePath));
        addResourcePack(server);
    }

    private static void addResourcePack(MinecraftServer server) {
        server.getResourcePackProperties().ifPresent(properties -> {
            PrunedData data = PrunedData.getServerState(server);
            if (!data.getResourcePackHash().equals(properties.hash())) {
                data.setResourcePackHash(properties.hash());
                try {
                    Path worldSavePath = server.getSavePath(WorldSavePath.ROOT);
                    Path downloadedPath = worldSavePath.resolve("resourcepack.zip");
                    URL url = URI.create(properties.url()).toURL();
                    try (var in = url.openStream()) {
                        Files.copy(in, downloadedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    uploadFile(server, downloadedPath);
                } catch (Exception e) {
                    PrunedMod.LOGGER.error("Failed to download resource pack: {}", e.getMessage());
                }
            } else {
                PrunedMod.LOGGER.info("Skipping up to date server resource pack");
            }
        });
    }

    private static void addNonRegionFiles(MinecraftServer server, Path currentPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    addNonRegionFiles(server, entry);
                } else if (Files.isRegularFile(entry) && !IgnoreList.isIgnored(entry)) {
                    PrunedData.getServerState(server).updateFile(entry);
                }
            }
        } catch (IOException e) {
            PrunedMod.LOGGER.info("Something went wrong trying to add file {} to the world download: {}", currentPath.toAbsolutePath(), e.getMessage());
        }
    }

    public static void uploadFile(MinecraftServer server, Path path) {
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

        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        Path relativePath = savePath.getParent().getParent().relativize(path);
        uploadExecutor.submit(() -> {
            try {
                WebDAVStorage.getInstance().uploadFile(path, relativePath);
                PrunedData.getServerState(server).updateSha1(path, newSha1);
                if (Config.debug) PrunedMod.LOGGER.info("Uploaded {}", relativePath);
            } catch (Exception e) {
                PrunedMod.LOGGER.error(e.getMessage());
                PrunedData.getServerState(server).updateSha1(path, "");
            }
        });
    }

    public static void removeFile(MinecraftServer server, Path path) {
        if (!WebDAVStorage.isConnected()) {
            PrunedMod.LOGGER.error("Removing file failed: Not connected to WebDAV server");
            return;
        }

        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        Path relativePath = savePath.getParent().getParent().relativize(path);
        removeExecutor.submit(() -> {
            try {
                WebDAVStorage.getInstance().removeWorldFile(relativePath);
                PrunedData.getServerState(server).removeFile(path);
                if (Config.debug) PrunedMod.LOGGER.info("Removed {}", relativePath);
            } catch (Exception e) {
                PrunedMod.LOGGER.error("Failed to remove {}: {}", relativePath, e.getMessage());
            }
        });
    }
}
