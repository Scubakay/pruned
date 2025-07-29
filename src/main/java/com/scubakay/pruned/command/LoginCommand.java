package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.storage.GoogleDriveStorage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class LoginCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> loginNode = CommandManager.literal("login")
                .executes(LoginCommand::login)
                .build();
        Commands.getRoot(dispatcher).addChild(loginNode);
    }

    private static int login(CommandContext<ServerCommandSource> source) {
        try {
            GoogleDriveStorage.login();
            source.getSource().sendFeedback(() -> Text.translatable("pruned.command.login.success"), false);
        } catch (Exception e) {
            source.getSource().sendError(Text.translatable("pruned.command.login.failed", e.getMessage()));
        }
        return 1;
    }
}
