package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.storage.WorldUploader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static com.scubakay.pruned.command.PermissionManager.*;

public class UploadCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> uploadNode = CommandManager.literal("upload")
                .requires(ctx -> hasPermission(ctx, UPLOAD_PERMISSION))
                .executes(UploadCommand::upload)
                .build();
        PrunedCommand.getRoot(dispatcher).addChild(uploadNode);
    }

    private static int upload(CommandContext<ServerCommandSource> source) {
        if (Config.debug) PrunedMod.LOGGER.info("Starting Pruned World upload...");
        WorldUploader.upload(source.getSource().getServer());
        return 1;
    }
}
