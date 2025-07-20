package com.scubakay.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.scubakay.data.BackupData;
import com.scubakay.storage.GoogleDriveStorage;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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
        } catch (IOException e) {
            source.getSource().sendError(Text.literal("Google Drive login failed: " + e.getMessage()));
        }
        return 1;
    }

    private static int upload(CommandContext<ServerCommandSource> source) {
        final Map<String, Path> regions = BackupData.getServerState().getRegions();
        boolean anyError = false;
        for (Map.Entry<String, Path> entry : regions.entrySet()) {
            String filePath = entry.getValue().toString();
            String subFolderName = filePath.contains("region") ? "region" : (filePath.contains("entities") ? "entities" : "other");
            try {
                GoogleDriveStorage.uploadFileToSubFolder(filePath, "application/octet-stream", subFolderName);
            } catch (IOException e) {
                anyError = true;
                source.getSource().sendError(Text.literal("Failed to upload " + entry.getKey() + ": " + e.getMessage()));
            }
        }
        if (!anyError) {
            source.getSource().sendFeedback(() -> Text.literal("All files uploaded successfully."), false);
        }
        return 1;
    }
}
