package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SaveCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> activateNode = CommandManager.literal("save")
                .executes(SaveCommand::activate)
                .build();
        Commands.getRoot(dispatcher).addChild(activateNode);

        LiteralCommandNode<ServerCommandSource> deactivateNode = CommandManager.literal("remove")
                .executes(SaveCommand::deactivate)
                .build();
        Commands.getRoot(dispatcher).addChild(deactivateNode);
    }

    private static int activate(CommandContext<ServerCommandSource> source) {
        source.getSource().sendFeedback(() -> Text.translatable("pruned.command.save", source.getSource().getServer().getName()), false);
        return 1;
    }

    private static int deactivate(CommandContext<ServerCommandSource> source) {
        source.getSource().sendFeedback(() -> Text.translatable("pruned.command.remove", source.getSource().getServer().getName()), false);
        return 1;
    }
}
