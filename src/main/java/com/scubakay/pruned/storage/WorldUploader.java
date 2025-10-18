package com.scubakay.pruned.storage;

import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.storage.webdav.task.RemoveFileTask;
import com.scubakay.pruned.storage.webdav.task.UploadFileTask;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldUploader {
    private static ExecutorService executor;
    private static ScheduledExecutorService intervalScheduler;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void initialize(MinecraftServer server) {
        if (Config.debug) PrunedMod.LOGGER.info("Initializing World upload executor");
        if (executor != null) {
            executor.shutdownNow();
        }
        if (initialized.compareAndSet(false, true)) {
            executor = Executors.newFixedThreadPool(Config.maxConcurrentUploads);
        }
        if (Config.uploadStrategy == Config.UploadStrategy.INTERVAL) {
            uploadOnInterval(server);
        }
    }

    public static void shutdown(MinecraftServer server) {
        if (Config.uploadStrategy == Config.UploadStrategy.SERVER_STOP) {
            upload(server);
        }

        if (Config.debug) PrunedMod.LOGGER.info("Shutting down World upload executor");
        if (executor != null) {
            if (Config.uploadStrategy != Config.UploadStrategy.SERVER_STOP && Config.stopUploadOnServerStop) {
                executor.shutdownNow();
            } else {
                executor.shutdown();
            }
            initialized.set(false);
        }

        if (intervalScheduler != null) {
            if (Config.uploadStrategy != Config.UploadStrategy.SERVER_STOP && Config.stopUploadOnServerStop) {
                intervalScheduler.shutdownNow();
            } else {
                intervalScheduler.shutdown();
            }
            intervalScheduler = null;
        }
    }

    public static void uploadOnInterval(MinecraftServer server) {
        if (intervalScheduler != null && !intervalScheduler.isShutdown()) {
            if (Config.debug) PrunedMod.LOGGER.error("Interval upload scheduler already running");
            return;
        }
        if (Config.debug) PrunedMod.LOGGER.info("Uploading world every {} minutes", Config.uploadInterval);
        intervalScheduler = Executors.newSingleThreadScheduledExecutor();
        intervalScheduler.scheduleAtFixedRate(() -> {
            try {
                upload(server);
            } catch (Exception e) {
                PrunedMod.LOGGER.error("Error during interval upload: {}", e.getMessage());
            }
        }, Config.uploadInterval, Config.uploadInterval, TimeUnit.MINUTES);
    }

    public static void upload(MinecraftServer server) {
        if (!PrunedData.getServerState(server).isActive()) {
            PrunedMod.LOGGER.error("Can't upload: Pruned is not active on this world.");
            return;
        }
        addResourcePack(server);
        addNonRegionFiles(server, server.getSavePath(WorldSavePath.ROOT));

        final Map<Path, String> files = PrunedData.getServerState(server).getFiles();
        files.forEach((path, sha1) -> uploadFile(server, path));
        if (Config.debug) PrunedMod.LOGGER.info("Scheduled {} files for upload", files.size());
    }

    private static void addResourcePack(MinecraftServer server) {
        server.getResourcePackProperties().ifPresent(properties -> {
            PrunedData data = PrunedData.getServerState(server);
            if (!data.getResourcePackHash().equals(properties.hash())) {
                data.setResourcePackHash(properties.hash());
                try {
                    Path worldSavePath = server.getSavePath(WorldSavePath.ROOT);
                    Path downloadedPath = worldSavePath.resolve("resourcepack.zip");
                    URL url = URI.create(properties.url()).normalize().toURL();
                    try (var in = url.openStream()) {
                        Files.copy(in, downloadedPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    PrunedData.getServerState(server).addFile(downloadedPath);
                    if (Config.debug) PrunedMod.LOGGER.info("Scheduled new resource pack upload with hash: {}", properties.hash());
                } catch (Exception e) {
                    PrunedMod.LOGGER.error("Failed to download resource pack: {}", e.getMessage());
                }
            }
        });
    }

    private static void addNonRegionFiles(MinecraftServer server, Path currentPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    addNonRegionFiles(server, entry);
                } else if (Files.isRegularFile(entry) && !IgnoreList.isIgnored(entry)) {
                    PrunedData.getServerState(server).addFile(entry);
                }
            }
        } catch (IOException e) {
            PrunedMod.LOGGER.info("Something went wrong trying to add file {} to the world download: {}", currentPath.toAbsolutePath(), e.getMessage());
        }
    }

    public static void uploadFile(MinecraftServer server, Path path) {
        if (executor == null) {
            if (Config.debug) PrunedMod.LOGGER.error("ExecutorService is not initialized! Cannot schedule upload for {}", path);
            return;
        }
        try {
            executor.submit(new UploadFileTask(server, path));
            if (Config.debug) PrunedMod.LOGGER.info("Scheduled upload for {}", path);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to schedule upload for {}: {}", path, e.getMessage());
        }
    }

    public static void removeFile(MinecraftServer server, Path path) {
        if (executor == null) {
            if (Config.debug) PrunedMod.LOGGER.error("ExecutorService is not initialized! Cannot schedule removal for {}", path);
            return;
        }
        try {
            executor.submit(new RemoveFileTask(server, path));
            if (Config.debug) PrunedMod.LOGGER.info("Scheduled removal for {}", path);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.error("Failed to schedule removal for {}: {}", path, e.getMessage());
        }
    }
}
