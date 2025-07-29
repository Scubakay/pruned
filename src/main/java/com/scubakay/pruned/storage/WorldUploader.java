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
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("CallToPrintStackTrace")
public class WorldUploader {
    // Single-threaded executor for uploads (can be changed to multi-threaded if needed)
    private static final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private static ScheduledExecutorService scheduler;

    /**
     * Returns the folder name (last directory) of the given path.
     * If the path points to a file, returns its parent folder name.
     */
    public static String getWorldName(Path path) {
        return path.getParent().getFileName().toString();
    }

    public static void scheduleWorldSync(MinecraftServer server) {
        PrunedData.setServer(server);
        if (!Config.autoSync || !PrunedData.getServerState().isActive()) {
            return;
        }

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
        String worldName = getWorldName(path);
        Path sourceRoot = path.toAbsolutePath();
        regions.forEach((regionName, regionPath) -> {
            Path absRegionPath = regionPath.toAbsolutePath();
            // Compute the relative path from the source root to the file
            Path relativePath = sourceRoot.relativize(absRegionPath);
            uploadRegionFile(worldName, absRegionPath, relativePath);
        });
    }

    public static void synchronizeWithIgnoreList(Path path) {
        List<Pattern> ignoredPatterns = Config.ignored.stream()
                .map(WorldUploader::gitignorePatternToRegex)
                .map(Pattern::compile)
                .collect(Collectors.toList());

        synchronizeRecursive(getWorldName(path), path, path, ignoredPatterns);
    }

    private static void synchronizeRecursive(String worldName, Path basePath, Path currentPath, List<Pattern> ignoredPatterns) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                String relativePath = basePath.relativize(entry).toString().replace("\\", "/");
                boolean ignored = ignoredPatterns.stream()
                        .anyMatch(p -> p.matcher(relativePath).matches());
                if (ignored) continue;

                if (Files.isDirectory(entry)) {
                    synchronizeRecursive(worldName, basePath, entry, ignoredPatterns);
                } else if (Files.isRegularFile(entry)) {
                    String mimeType = Files.probeContentType(entry);
                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }
                    // Submit upload task to executor
                    String finalMimeType = mimeType;
                    uploadFile(worldName, entry, finalMimeType, relativePath);
                }
            }
        } catch (IOException e) {
            PrunedMod.LOGGER.info("Something went wrong trying to upload {} recursively", currentPath.toAbsolutePath());
            e.printStackTrace();
        }
    }

    private static void uploadFile(String worldName, Path entry, String finalMimeType, String relativePath) {
        uploadExecutor.submit(() -> {
            try {
                GoogleDriveStorage.uploadFileToSubFolderWithPath(
                        entry.toAbsolutePath().toString(),
                        finalMimeType,
                        worldName,
                        relativePath
                );
                if (Config.debug) {
                    PrunedMod.LOGGER.info("Synchronized {}", entry.toAbsolutePath());
                }
            } catch (IOException e) {
                PrunedMod.LOGGER.info("Something went wrong trying to upload {}", entry.toAbsolutePath());
                e.printStackTrace();
            }
        });
    }

    // Upload a single file to Google Drive, preserving its relative path under worldName
    private static void uploadRegionFile(String worldName, Path filePath, Path relativePath) {
        if (Files.isRegularFile(filePath)) {
            String mimeType;
            try {
                mimeType = Files.probeContentType(filePath);
            } catch (IOException e) {
                mimeType = "application/octet-stream";
            }
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            // Submit upload task to executor
            String finalMimeType = mimeType;
            uploadFile(worldName, filePath, finalMimeType, relativePath.toString().replace("\\", "/"));
        }
    }

    // Converts .gitignore-like patterns to regex
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
