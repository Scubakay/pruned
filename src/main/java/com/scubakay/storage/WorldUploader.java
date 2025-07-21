package com.scubakay.storage;

import com.scubakay.config.Config;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WorldUploader {
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
                    // Pass relativePath to preserve folder structure
                    GoogleDriveStorage.uploadFileToSubFolderWithPath(
                        entry.toAbsolutePath().toString(),
                        mimeType,
                        worldName,
                        relativePath
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            try {
                GoogleDriveStorage.uploadFileToSubFolderWithPath(
                    filePath.toAbsolutePath().toString(),
                    mimeType,
                    worldName,
                    relativePath.toString().replace("\\", "/")
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                case '*': sb.append(".*"); break;
                case '?': sb.append('.'); break;
                case '.': sb.append("\\."); break;
                case '/': sb.append("/"); break;
                default: sb.append(Pattern.quote(String.valueOf(c))); break;
            }
        }

        sb.append("$");
        return sb.toString();
    }

    /**
     * Returns the folder name (last directory) of the given path.
     * If the path points to a file, returns its parent folder name.
     */
    public static String getWorldName(Path path) {
        return path.getParent().getFileName().toString();
    }
}
