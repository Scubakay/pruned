package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.storage.WorldUploader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.scubakay.pruned.command.PermissionManager.CONFIGURE_PERMISSION;
import static com.scubakay.pruned.command.PermissionManager.hasPermission;

public class UploadCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> uploadNode = CommandManager.literal("upload")
                .requires(ctx -> hasPermission(ctx, CONFIGURE_PERMISSION))
                .executes(UploadCommand::upload)
                .build();
        Commands.getRoot(dispatcher).addChild(uploadNode);
    }

    private static int upload(CommandContext<ServerCommandSource> source) {
        WorldUploader.afterSave(source.getSource().getServer(), false, false);
        return 1;
    }
}
