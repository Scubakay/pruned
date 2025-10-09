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
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("CallToPrintStackTrace")
public class WorldUploader {
    // Single-threaded executor for uploads (can be changed to multithreaded if needed)
    private static final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService removeExecutor = Executors.newSingleThreadExecutor();

    // Track files currently queued or uploading
    private static final Set<String> uploadingFiles = ConcurrentHashMap.newKeySet();
    private static final Set<String> removingFiles = ConcurrentHashMap.newKeySet();

    public static void upload(MinecraftServer server, boolean ignoredFlush, boolean ignoredForce) {
        final PrunedData serverState = PrunedData.getServerState(server);
        if (!Config.autoSync || !serverState.isActive()) {
            return;
        }

        Path path = server.getSavePath(WorldSavePath.ROOT);
        synchronizeWithIgnoreList(server, path);
        uploadWorldFiles(server);
    }

    public static void uploadWorldFiles(MinecraftServer server) {
        Map<Path, String> files = PrunedData.getServerState(server).getFiles();
        files.forEach((path, sha1) -> {
            String newSha1 = getSha1(path);
            if (newSha1 == null) {
                if (Config.debug) PrunedMod.LOGGER.info("Could not get sha1 for {}", path);
                return;
            }
            if (!newSha1.equals(sha1)) {
                uploadWorldFile(server, path, newSha1);
            }
        });
    }

    public static void synchronizeWithIgnoreList(MinecraftServer server, Path path) {
        synchronizeRecursive(server, path);
    }

    private static void synchronizeRecursive(MinecraftServer server, Path currentPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    synchronizeRecursive(server, entry);
                } else if (Files.isRegularFile(entry) && !isIgnored(entry)) {
                    PrunedData.getServerState(server).updateFile(entry.normalize());
                }
            }
        } catch (IOException e) {
            PrunedMod.LOGGER.info("Something went wrong trying to add file {} to the world download", currentPath.toAbsolutePath());
            e.printStackTrace();
        }
    }

    public static void uploadWorldFile(MinecraftServer server, Path path, String newSha1) {
        if (!WebDAVStorage.isConnected()) return;

        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        Path relativePath = savePath.getParent().getParent().relativize(path);
        if (uploadingFiles.add(path.toString())) {
            uploadExecutor.submit(() -> {
                try {
                    WebDAVStorage.getInstance().uploadFile(path, relativePath);
                } finally {
                    uploadingFiles.remove(path.toString());
                    PrunedData.getServerState(server).updateSha1(path, newSha1);
                }
            });
        }
    }

    public static void removeFile(MinecraftServer server, Path path) {
        if (!WebDAVStorage.isConnected()) return;

        uploadingFiles.remove(path.toString());
        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        Path relativePath = savePath.getParent().getParent().relativize(path);
        if (removingFiles.add(path.toString())) {
            removeExecutor.submit(() -> {
                try {
                    WebDAVStorage.getInstance().removeWorldFile(relativePath);
                } finally {
                    removingFiles.remove(path.toString());
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

    private static String getSha1(Path path) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] hashBytes = MessageDigest.getInstance("SHA-1").digest(fileBytes);
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return null;
        }
    }
}
