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
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class DynamicDialog {
    private final String fileName;
    private JsonObject json;

    private DynamicDialog(String fileName) throws IOException {
        this.fileName = fileName;
        getDialogJson();
    }

    public static DynamicDialog create(String fileName) {
        try {
            return new DynamicDialog(fileName);
        } catch (IOException e) {
            PrunedMod.LOGGER.error("Failed to create dialog: {}", e.getMessage());
            return null;
        }
    }

    public void show(CommandContext<ServerCommandSource> context) {
        this.show(context, Map.of());
    }

    public void show(CommandContext<ServerCommandSource> context, Map<String, String> replacements) {
        try {
            String finalDialogJson = parseDialogJson(replacements);
            showDialogUsingServerCommandSource(context, finalDialogJson);
        } catch (Exception e) {
            if (Config.debug) PrunedMod.LOGGER.info("Could not parse dialog json: {}", e.getMessage());
        }

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

    public static void showStatic(CommandContext<ServerCommandSource> context, Identifier id) {
        showDialogUsingServerCommandSource(context, id.toString());
    }

    private static void showDialogUsingServerCommandSource(CommandContext<ServerCommandSource> context, String dialog) {
        String playerName = Objects.requireNonNull(context.getSource().getPlayer()).getNameForScoreboard();
        String command = String.format("dialog show %s %s", playerName, dialog);
        ServerCommandSource executor = context.getSource().getServer() != null
                ? context.getSource().getServer().getCommandSource()
                : context.getSource();
        try {
            context.getSource().getDispatcher().execute(command, executor);
        } catch (CommandSyntaxException e) {
            PrunedMod.LOGGER.error("Failed showing static dialog: {}", e.getMessage());
        }
    }
}
