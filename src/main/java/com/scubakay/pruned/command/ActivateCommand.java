package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.storage.WorldUploader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.scubakay.pruned.command.PermissionManager.CONFIGURE_PERMISSION;
import static com.scubakay.pruned.command.PermissionManager.hasPermission;

public class ActivateCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> activateNode = CommandManager.literal("activate")
                .requires(ctx -> hasPermission(ctx, CONFIGURE_PERMISSION))
                .executes(ActivateCommand::activate)
                .build();
        PrunedCommand.getRoot(dispatcher).addChild(activateNode);

        LiteralCommandNode<ServerCommandSource> deactivateNode = CommandManager.literal("deactivate")
                .requires(ctx -> hasPermission(ctx, CONFIGURE_PERMISSION))
                .executes(ActivateCommand::deactivate)
                .build();
        PrunedCommand.getRoot(dispatcher).addChild(deactivateNode);
    }

    private static int activate(CommandContext<ServerCommandSource> source) {
        PrunedData.getServerState(source.getSource().getServer()).activate();
        WorldUploader.afterSave(source.getSource().getServer(), false, false);
        source.getSource().sendFeedback(() -> Text.translatable("pruned.command.activate", source.getSource().getServer().getName()), false);
        return 1;
    }

    private static int deactivate(CommandContext<ServerCommandSource> source) {
        PrunedData.getServerState(source.getSource().getServer()).deactivate();
        source.getSource().sendFeedback(() -> Text.translatable("pruned.command.deactivate", source.getSource().getServer().getName()), false);
        return 1;
    }
}
