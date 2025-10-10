package com.scubakay.pruned.util;

import com.scubakay.pruned.config.Config;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class IgnoreList {
    public static boolean isIgnored(Path relativePath) {
        List<Pattern> ignoredPatterns = Config.ignored.stream()
                .map(IgnoreList::gitignorePatternToRegex)
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
