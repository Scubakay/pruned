package com.scubakay.storage;

import com.scubakay.config.Config;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WorldUploader {
    public static void Synchronize(String worldName, Path path) {
        List<Pattern> ignoredPatterns = Config.ignored.stream()
            .map(WorldUploader::gitignorePatternToRegex)
            .map(Pattern::compile)
            .collect(Collectors.toList());

        synchronizeRecursive(worldName, path, path, ignoredPatterns);
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
}
