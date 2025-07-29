package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Collections;

public class Commands {
    public static final String ROOT_COMMAND = "pruned";

    public static CommandNode<ServerCommandSource> getRoot(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> root = dispatcher.findNode(Collections.singleton(ROOT_COMMAND));
        if (root == null) {
            root = dispatcher.register(CommandManager.literal(ROOT_COMMAND));
            //dispatcher.register(CommandManager.literal(ALIAS_COMMAND).redirect(root));
        }
        return root;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry, CommandManager.RegistrationEnvironment environment) {
        LoginCommand.register(dispatcher, registry, environment);
        ActivateCommand.register(dispatcher, registry, environment);
        UploadCommand.register(dispatcher, registry, environment);
    }
}
