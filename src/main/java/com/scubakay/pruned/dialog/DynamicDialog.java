package com.scubakay.pruned.dialog;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.command.PrunedCommand;
import com.scubakay.pruned.config.Config;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DynamicDialog {
    private final String fileName;
    private JsonObject json;

    private DynamicDialog(String fileName) throws IOException {
        this.fileName = fileName;
        getDialogJson();
    }

    public static DynamicDialog create(String fileName) throws IOException {
        return new DynamicDialog(fileName);
    }

    public void show(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        this.show(context, null);
    }

    public void show(CommandContext<ServerCommandSource> context, Map<String, String> replacements) throws CommandSyntaxException {
        String finalDialogJson = "error";
        try {
            if (replacements != null) finalDialogJson = parseDialogJson(replacements);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.info("Could not parse dialog json: {}", e.getMessage());
        }
        String command = String.format("dialog show @s %s", finalDialogJson);
        context.getSource().getDispatcher().execute(command, context.getSource());
    }

    public void getDialogJson() throws IOException {
        InputStream is = DynamicDialog.class.getClassLoader()
                .getResourceAsStream("assets/pruned/dialog/" + fileName + ".json");
        if (is == null) throw new IOException("Dialog file not found: " + fileName);
        Gson gson = new Gson();
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            this.json = gson.fromJson(reader, JsonObject.class);
        }
    }

    public String parseDialogJson(Map<String, String> replacements) {
        Gson gson = new Gson();
        String parsed = gson.toJson(this.json);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            parsed = parsed.replace(entry.getKey(), entry.getValue());
        }
        return parsed;
    }

    public void addDialogAction(String filename) {
        InputStream is = PrunedCommand.class.getClassLoader().getResourceAsStream(
                "assets/pruned/dialog/" + filename + ".json");
        if (is != null) {
            JsonElement webdavButton = new com.google.gson.Gson().fromJson(new InputStreamReader(is), JsonElement.class);
            JsonArray actions = json.has("actions") && json.get("actions").isJsonArray()
                    ? json.getAsJsonArray("actions")
                    : new JsonArray();
            actions.add(webdavButton);
            json.add("actions", actions);
        }
    }
}
