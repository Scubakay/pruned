package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.data.ScoreboardManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CheckCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> checkNode = CommandManager.literal("check")
                .executes(CheckCommand::toggleCheck)
                .build();
        Commands.getRoot(dispatcher).addChild(checkNode);
    }

    private static int toggleCheck(CommandContext<ServerCommandSource> context) {
        boolean prunedCheck = ScoreboardManager.toggleBooleanScore(context.getSource().getPlayer(), ScoreboardManager.PRUNED_CHECK_SCOREBOARD);
        context.getSource().sendFeedback(() -> Text.literal(String.format("Pruned region check %s",prunedCheck ? "enabled" : "disabled")), false);
        return 1;
    }
}
