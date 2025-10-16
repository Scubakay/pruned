package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.dialog.DynamicDialog;
import com.scubakay.pruned.domain.PrunedServerPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.Collections;
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
            PrunedServerPlayerEntity player = (PrunedServerPlayerEntity) context.getSource().getPlayer();
            if (player == null) {
                context.getSource().sendError(Text.literal("This command is only client side only"));
                return 0;
            }

            if (!PrunedData.getServerState(context.getSource().getServer()).isActive()) {
                if (PermissionManager.hasPermission(context.getSource(), PermissionManager.CONFIGURE_PERMISSION)) {
                    DynamicDialog.create("activate").show(context);
                } else {
                    context.getSource().sendError(Text.literal("Pruned: Insufficient permissions"));
                }
            } else {
                DynamicDialog dialogObj = DynamicDialog.create("pruned");
                if (PermissionManager.hasPermission(context.getSource(), PermissionManager.TRIGGER_UPLOAD_PERMISSION)) {
                    dialogObj.addDialogAction("upload_button");
                }
                if (PermissionManager.hasPermission(context.getSource(), PermissionManager.CONFIGURE_PERMISSION)) {
                    dialogObj.addDialogAction("webdav_config_button");
                }

                dialogObj.show(context, Map.of(
                    "%REGION_IN_WORLD_DOWNLOAD%", player.pruned$isRegionSaved() ? "Yes" : "No",
                    "%REGION_IN_WORLD_DOWNLOAD_COLOR%", player.pruned$isRegionSaved() ? "green" : "yellow",
                    "%ADD_OR_REMOVE%", player.pruned$isRegionSaved() ? "Remove" : "Add",
                    "%ADD_OR_REMOVE_COLOR%", player.pruned$isRegionSaved() ? "red" : "green",
                    "%ADD_OR_REMOVE_TOOLTIP%", player.pruned$isRegionSaved() ? "Remove the current region from the world download" : "Add the current region to the world download",
                    "%PRUNED_SAVE_OR_REMOVE_COMMAND%", player.pruned$isRegionSaved() ? "pruned remove" : "pruned save",
                    "%ENABLE_DISABLE_HELPER%", player.pruned$isRegionHelperEnabled() ? "Disable Helper" : "Enable Helper",
                    "%ENABLE_DISABLE_HELPER_COLOR%", player.pruned$isRegionHelperEnabled() ? "red" : "green"
                ));
            }
            return 1;


        } catch (NullPointerException | IOException e) {
            context.getSource().sendError(Text.literal("Failed to load dialog: " + e.getMessage()));
            return 0;
        }
    }
}
