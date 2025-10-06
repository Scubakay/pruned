package com.scubakay.pruned.storage;

import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("CallToPrintStackTrace")
public class WorldUploader {
    // Single-threaded executor for uploads (can be changed to multithreaded if needed)
    private static final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private static ScheduledExecutorService scheduler;

    // Track files currently queued or uploading
    private static final Set<String> uploadingFiles = ConcurrentHashMap.newKeySet();

    public static void scheduleWorldSync(MinecraftServer server) {
        PrunedData.setServer(server);

        Path path = server.getSavePath(WorldSavePath.ROOT);
        scheduler = Executors.newScheduledThreadPool(2);

        if (Config.debug) {
            PrunedMod.LOGGER.info("Uploading updated regions every {} seconds", Config.regionSyncInterval);
        }

        // Schedule region sync
        scheduler.scheduleAtFixedRate(
                () -> synchronizeDirty(path, PrunedData.getServerState(server).getRegions()),
                Config.regionSyncInterval, // initial delay
                Config.regionSyncInterval, // period
                TimeUnit.SECONDS
        );

        if (Config.debug) {
            PrunedMod.LOGGER.info("Uploading non-region world files every {} seconds", Config.worldSyncInterval);
        }

        // Schedule world sync
        scheduler.scheduleAtFixedRate(
                () -> synchronizeWithIgnoreList(path),
                Config.worldSyncInterval, // initial delay
                Config.worldSyncInterval, // period
                TimeUnit.SECONDS
        );
    }

    public static void scheduleWorldSyncEnd(MinecraftServer ignoredServer) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown(); // Allow running tasks to finish
        }
        if (Config.debug) {
            PrunedMod.LOGGER.info("Stopping world uploads. Currently running uploads will continue.");
        }
    }

    public static void synchronizeDirty(Path path, Map<String, Path> regions) {
        if (!Config.autoSync || !PrunedData.getServerState().isActive()) {
            return;
        }
        regions.forEach((regionName, regionPath) -> {
            Path relativePath = path.getParent().relativize(regionPath);
            uploadFile(regionPath, relativePath);
        });
        regions.clear();
    }

    public static void synchronizeWithIgnoreList(Path path) {
        if (!Config.autoSync || !PrunedData.getServerState().isActive()) {
            return;
        }
        synchronizeRecursive(path.getParent(), path);
    }

    private static void synchronizeRecursive(Path basePath, Path currentPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                Path relativePath = basePath.relativize(entry);

                if (!isIgnored(relativePath)) {
                    if (Files.isDirectory(entry)) {
                        synchronizeRecursive(basePath, entry);
                    } else if (Files.isRegularFile(entry)) {
                        uploadFile(entry, relativePath);
                    }
                }
            }
        } catch (IOException e) {
            PrunedMod.LOGGER.info("Something went wrong trying to upload {} recursively", currentPath.toAbsolutePath());
            e.printStackTrace();
        }
    }

    public static void uploadFile(Path filePath, Path relativePath) {
        if (uploadingFiles.add(filePath.toString())) {
            uploadExecutor.submit(() -> {
                try {
                    WebDAVStorage.getInstance().uploadWorldFile(filePath, relativePath);
                } finally {
                    uploadingFiles.remove(filePath.toString());
                }
            });
        }
    }

    private static boolean isIgnored(Path relativePath) {
        List<Pattern> ignoredPatterns = Config.ignored.stream()
                .map(WorldUploader::gitignorePatternToRegex)
                .map(Pattern::compile)
                .toList();
        return ignoredPatterns.stream().anyMatch(p -> p.matcher(relativePath.toString()).matches());
    }

    private static String gitignorePatternToRegex(String pattern) {
        StringBuilder sb = new StringBuilder();
        boolean anchored = pattern.startsWith("/");
        String corePattern = anchored ? pattern.substring(1) : pattern;

        sb.append(anchored ? "^" : ".*");

        for (int i = 0; i < corePattern.length(); i++) {
            char c = corePattern.charAt(i);
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append('.');
                    break;
                case '.':
                    sb.append("\\.");
                    break;
                case '/':
                    sb.append("/");
                    break;
                default:
                    sb.append(Pattern.quote(String.valueOf(c)));
                    break;
            }
        }

        sb.append("$");
        return sb.toString();
    }
}
