package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.storage.MegaStorage;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class LoginCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> loginNode = CommandManager.literal("login")
                .then(CommandManager.argument("username", StringArgumentType.string())
                    .then(CommandManager.argument("password", StringArgumentType.string())
                        .executes(LoginCommand::login)))
                .build();
        Commands.getRoot(dispatcher).addChild(loginNode);
    }

    private static int login(CommandContext<ServerCommandSource> context) {
        String username = StringArgumentType.getString(context, "username");
        String password = StringArgumentType.getString(context, "password");
        try {
            MegaStorage.getInstance().setCredentials(username, password);
            MegaStorage.getInstance().login();
            context.getSource().sendFeedback(() -> Text.translatable("pruned.command.login.success"), false);
        } catch (Exception e) {
            context.getSource().sendError(Text.translatable("pruned.command.login.failed", e.getMessage()));
        }
        return 1;
    }
}
