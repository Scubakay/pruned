package com.scubakay.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.scubakay.storage.WorldUploader;
import com.scubakay.storage.GoogleDriveStorage;
import com.google.api.client.http.javanet.NetHttpTransport;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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
            // Attempt to login (will prompt user if needed)
            GoogleDriveStorage.getCredentials(new NetHttpTransport());
            source.getSource().sendFeedback(() -> Text.literal("Google Drive login successful or already authorized."), false);
        } catch (Exception e) {
            source.getSource().sendError(Text.literal("Google Drive login failed: " + e.getMessage()));
        }
        return 1;
    }

    private static int upload(CommandContext<ServerCommandSource> source) {
        Path path = Path.of("F:\\projects\\pruned\\run\\saves\\New World");
        WorldUploader.Synchronize("New World", path);
        return 1;
    }
}
