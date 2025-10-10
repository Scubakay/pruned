package com.scubakay.pruned.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.scubakay.pruned.domain.PrunedServerPlayerEntity;
import com.scubakay.pruned.util.DynamicDialogs;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PrunedCommand {
    public static final String ROOT_COMMAND = "pruned";

    public static CommandNode<ServerCommandSource> getRoot(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> root = dispatcher.findNode(Collections.singleton(ROOT_COMMAND));
        if (root == null) {
            root = dispatcher.register(CommandManager.literal(ROOT_COMMAND)
                    .executes(PrunedCommand::openPrunedDialog)
            );
            //dispatcher.register(CommandManager.literal(ALIAS_COMMAND).redirect(root));
        }
        return root;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment environment) {
        ActivateCommand.register(dispatcher, registry, environment);
        UploadCommand.register(dispatcher, registry, environment);
        SaveCommand.register(dispatcher, registry, environment);
        HelperCommand.register(dispatcher, registry, environment);
        WebDavCommand.register(dispatcher, registry, environment);
    }

    private static int openPrunedDialog(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            Map<String, String> replacements = new HashMap<>();
            PrunedServerPlayerEntity player = (PrunedServerPlayerEntity) context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendError(Text.literal("This command is only client side only"));
                return 0;
            }
            replacements.put("%REGION_IN_WORLD_DOWNLOAD%", player.pruned$isRegionSaved() ? "Yes" : "No");
            replacements.put("%REGION_IN_WORLD_DOWNLOAD_COLOR%", player.pruned$isRegionSaved() ? "green" : "yellow");
            replacements.put("%ADD_OR_REMOVE%", player.pruned$isRegionSaved() ? "Remove" : "Add");
            replacements.put("%ADD_OR_REMOVE_COLOR%", player.pruned$isRegionSaved() ? "red" : "green");
            replacements.put("%ADD_OR_REMOVE_TOOLTIP%", player.pruned$isRegionSaved() ? "Remove the current region from the world download" : "Add the current region to the world download");
            replacements.put("%PRUNED_SAVE_OR_REMOVE_COMMAND%", player.pruned$isRegionSaved() ? "pruned remove" : "pruned save");
            replacements.put("%ENABLE_DISABLE_HELPER%", player.pruned$isRegionHelperEnabled() ? "Disable Helper" : "Enable Helper");
            replacements.put("%ENABLE_DISABLE_HELPER_COLOR%", player.pruned$isRegionHelperEnabled() ? "red" : "green");

            boolean hasConfigurePermission = PermissionManager.hasPermission(context.getSource(), PermissionManager.CONFIGURE_PERMISSION);

            JsonObject dialogObj = DynamicDialogs.getDialogJson("pruned");

            if (hasConfigurePermission) {
                InputStream is = PrunedCommand.class.getClassLoader().getResourceAsStream(
                        "assets/pruned/dialog/webdav_config_button.json");
                if (is != null) {
                    JsonElement webdavButton = new com.google.gson.Gson().fromJson(new InputStreamReader(is), JsonElement.class);
                    JsonArray actions = dialogObj.has("actions") && dialogObj.get("actions").isJsonArray()
                            ? dialogObj.getAsJsonArray("actions")
                            : new JsonArray();
                    actions.add(webdavButton);
                    dialogObj.add("actions", actions);
                }
            }

            String finalDialogJson = DynamicDialogs.parseDialogJson(dialogObj, replacements);
            String command = String.format("dialog show @s %s", finalDialogJson);
            context.getSource().getDispatcher().execute(command, context.getSource());
            return 1;
        } catch (NullPointerException | IOException e) {
            context.getSource().sendError(Text.literal("Failed to load dialog: " + e.getMessage()));
            return 0;
        }
    }
}
