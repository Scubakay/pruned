package com.scubakay.pruned.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class DynamicDialogs {
    /**
     * Loads a dialog JSON file and replaces all instances of specified placeholders.
     * @param fileName The dialog file name (without .json extension)
     * @param replacements Map of placeholders to replacement values
     * @return The dialog JSON as a string with replaced values
     */
    public static String getDialogJson(String fileName, Map<String, String> replacements) throws IOException {
        InputStream is = DynamicDialogs.class.getClassLoader()
                .getResourceAsStream("assets/pruned/dialog/" + fileName + ".json");
        if (is == null) throw new IOException("Dialog file not found: " + fileName);
        String json;
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            json = scanner.useDelimiter("\\A").next();
        }
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            json = json.replace(entry.getKey(), entry.getValue());
        }
        return json;
    }
}
