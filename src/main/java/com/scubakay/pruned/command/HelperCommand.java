package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.domain.PrunedServerPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.scubakay.pruned.command.PermissionManager.*;

public class HelperCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> checkNode = CommandManager.literal("helper")
                .requires(ctx -> hasPermission(ctx, REGION_PERMISSION))
                .executes(HelperCommand::toggleCheck)
                .build();
        PrunedCommand.getRoot(dispatcher).addChild(checkNode);
    }

    private static int toggleCheck(CommandContext<ServerCommandSource> context) {
        PrunedServerPlayerEntity player = (PrunedServerPlayerEntity) context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("This command is only client side only"));
            return 0;
        }
        player.pruned$toggleRegionHelper();
        context.getSource().sendFeedback(() -> Text.literal(String.format("Pruned region helper %s",player.pruned$isRegionHelperEnabled() ? "enabled" : "disabled")), false);
        return 1;
    }
}
