package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.storage.WorldUploader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

public class UploadCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> uploadNode = CommandManager.literal("upload")
                .executes(UploadCommand::upload)
                .build();
        Commands.getRoot(dispatcher).addChild(uploadNode);
    }

    private static int upload(CommandContext<ServerCommandSource> source) {
        Path path = source.getSource().getServer().getSavePath(WorldSavePath.ROOT).getParent();
        WorldUploader.synchronizeDirty(path, PrunedData.getServerState(source.getSource().getServer()).getRegions());
        WorldUploader.synchronizeWithIgnoreList(path);
        return 1;
    }
}
