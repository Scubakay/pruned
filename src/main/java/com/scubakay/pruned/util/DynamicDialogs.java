package com.scubakay.pruned.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DynamicDialogs {
    /**
     * Loads a dialog JSON file and returns it as a JsonObject.
     * @param fileName The dialog file name (without .json extension)
     * @return The dialog JSON as a JsonObject
     */
    public static JsonObject getDialogJson(String fileName) throws IOException {
        InputStream is = DynamicDialogs.class.getClassLoader()
                .getResourceAsStream("assets/pruned/dialog/" + fileName + ".json");
        if (is == null) throw new IOException("Dialog file not found: " + fileName);
        Gson gson = new Gson();
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    /**
     * Replaces placeholders in a dialog JsonObject and returns the final JSON string.
     *
     * @param dialog       The dialog JsonObject
     * @param replacements Map of placeholders to replacement values
     * @return The dialog JSON as a string with replaced values
     */
    public static String parseDialogJson(JsonObject dialog, Map<String, String> replacements) {
        Gson gson = new Gson();
        String json = gson.toJson(dialog);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            json = json.replace(entry.getKey(), entry.getValue());
        }
        return json;
    }
}
