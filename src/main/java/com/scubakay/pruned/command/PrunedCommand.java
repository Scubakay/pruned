package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.dialog.DynamicDialog;
import com.scubakay.pruned.dialog.PrunedDialog;
import com.scubakay.pruned.dialog.WebDavConfigDialog;
import com.scubakay.pruned.domain.PrunedServerPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;

public class PrunedCommand {
    public static final String ROOT_COMMAND = "pruned";

    public static CommandNode<ServerCommandSource> getRoot(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> root = dispatcher.findNode(Collections.singleton(ROOT_COMMAND));
        if (root == null) {
            root = dispatcher.register(CommandManager.literal(ROOT_COMMAND)
                    .executes(PrunedCommand::openDialog)
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

    private static int openDialog(CommandContext<ServerCommandSource> context) {
        PrunedServerPlayerEntity player = (PrunedServerPlayerEntity) context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command is only client side only"));
            return 0;
        }

        if (!PrunedData.getServerState(context.getSource().getServer()).isActive()) {
            if (PermissionManager.hasPermission(context.getSource(), PermissionManager.CONFIGURE_PERMISSION)) {
                if (Config.webDavEndpoint == null || Config.webDavEndpoint.isEmpty() ||
                        Config.webDavUsername == null || Config.webDavUsername.isEmpty() ||
                        Config.webDavPassword == null || Config.webDavPassword.isEmpty()
                ) {
                    WebDavConfigDialog.openWebDavConfigDialog(context);
                } else {
                    DynamicDialog.showStatic(context, Identifier.of(PrunedMod.MOD_ID, "activate"));
                }
            } else {
                DynamicDialog.showStatic(context, Identifier.of(PrunedMod.MOD_ID, "no_permissions"));
            }
        } else {
            PrunedDialog.openPrunedDialog(context, player);
        }
        return 1;
    }
}
