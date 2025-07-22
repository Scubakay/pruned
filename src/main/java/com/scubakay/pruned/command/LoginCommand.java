package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.scubakay.pruned.data.BackupData;
import com.scubakay.pruned.storage.WorldUploader;
import com.scubakay.pruned.storage.GoogleDriveStorage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;

public class LoginCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        dispatcher.register(CommandManager.literal("pruned")
                .then(CommandManager.literal("login")
                        .executes(LoginCommand::login))
                .then(CommandManager.literal("upload")
                        .executes(LoginCommand::upload)));
    }

    private static int login(CommandContext<ServerCommandSource> source) {
        try {
            GoogleDriveStorage.login();
            source.getSource().sendFeedback(() -> Text.literal("Google Drive login successful or already authorized."), false);
        } catch (Exception e) {
            source.getSource().sendError(Text.literal("Google Drive login failed: " + e.getMessage()));
        }
        return 1;
    }

    private static int upload(CommandContext<ServerCommandSource> source) {
        Path path = source.getSource().getServer().getSavePath(WorldSavePath.ROOT);
        WorldUploader.synchronizeDirty(path, BackupData.getServerState(source.getSource().getServer()).getRegions());
        WorldUploader.synchronizeWithIgnoreList(path);
        return 1;
    }
}
