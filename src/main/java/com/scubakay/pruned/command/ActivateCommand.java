package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.data.PrunedData;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ActivateCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> activateNode = CommandManager.literal("activate")
                .executes(ActivateCommand::activate)
                .build();
        Commands.getRoot(dispatcher).addChild(activateNode);

        LiteralCommandNode<ServerCommandSource> deactivateNode = CommandManager.literal("deactivate")
                .executes(ActivateCommand::deactivate)
                .build();
        Commands.getRoot(dispatcher).addChild(deactivateNode);
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
}
