package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.storage.WorldUploader;
import com.scubakay.pruned.storage.GoogleDriveStorage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

public class PrunedCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        dispatcher.register(CommandManager.literal("pruned")
                .then(CommandManager.literal("login")
                        .executes(PrunedCommand::login))
                .then(CommandManager.literal("upload")
                        .executes(PrunedCommand::upload))
                .then(CommandManager.literal("activate")
                        .executes(PrunedCommand::activate))
                .then(CommandManager.literal("deactivate")
                        .executes(PrunedCommand::deactivate))
        );
    }

    private static int activate(CommandContext<ServerCommandSource> source) {
        PrunedData.getServerState(source.getSource().getServer()).activate();
        source.getSource().sendFeedback(() -> Text.translatable("pruned.command.activate", source.getSource().getServer().getName()), false);
        return 1;
    }

    private static int deactivate(CommandContext<ServerCommandSource> source) {
        PrunedData.getServerState(source.getSource().getServer()).deactivate();
        source.getSource().sendFeedback(() -> Text.translatable("pruned.command.deactivate", source.getSource().getServer().getName()), false);
        return 1;
    }

    private static int login(CommandContext<ServerCommandSource> source) {
        try {
            GoogleDriveStorage.login();
            source.getSource().sendFeedback(() -> Text.literal("pruned.command.login.success"), false);
        } catch (Exception e) {
            source.getSource().sendError(Text.translatable("pruned.command.login.failed", e.getMessage()));
        }
        return 1;
    }

    private static int upload(CommandContext<ServerCommandSource> source) {
        Path path = source.getSource().getServer().getSavePath(WorldSavePath.ROOT);
        WorldUploader.synchronizeDirty(path, PrunedData.getServerState(source.getSource().getServer()).getRegions());
        WorldUploader.synchronizeWithIgnoreList(path);
        return 1;
    }
}
