package com.scubakay.pruned.util;

import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class FileHasher {
    public static String getSha1(Path path) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] hashBytes = MessageDigest.getInstance("SHA-1").digest(fileBytes);
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.info("Could not get sha1 for {}", path);
            return "";
        }
    }
}
