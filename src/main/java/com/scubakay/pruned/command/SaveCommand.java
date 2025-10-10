package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.storage.WorldUploader;
import com.scubakay.pruned.util.PositionHelpers;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.domain.RegionPos;
import com.scubakay.pruned.domain.PrunedServerPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.nio.file.Path;

import static com.scubakay.pruned.command.PermissionManager.*;

public class SaveCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> activateNode = CommandManager.literal("save")
                .requires(ctx -> hasPermission(ctx, SAVE_REGION_PERMISSION))
                .executes(SaveCommand::save)
                .build();
        PrunedCommand.getRoot(dispatcher).addChild(activateNode);

        LiteralCommandNode<ServerCommandSource> deactivateNode = CommandManager.literal("remove")
                .requires(ctx -> hasPermission(ctx, REMOVE_REGION_PERMISSION))
                .executes(SaveCommand::remove)
                .build();
        PrunedCommand.getRoot(dispatcher).addChild(deactivateNode);
    }

    private static int save(CommandContext<ServerCommandSource> source) {
        ServerPlayerEntity player = source.getSource().getPlayer();
        if (player == null) {
            source.getSource().sendError(Text.literal("This command is only client side only"));
            return 0;
        }
        RegionPos pos = RegionPos.from(player.getChunkPos());
        addRegionToPrunedData(source, pos);
        ((PrunedServerPlayerEntity) player).pruned$loadPrunedStatus(player, pos);
        source.getSource().sendFeedback(() -> Text.literal(String.format("Added current region (%s) to Pruned world download", pos)), false);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> source) {
        ServerPlayerEntity player = source.getSource().getPlayer();
        if (player == null) {
            source.getSource().sendError(Text.literal("This command is only client side only"));
            return 0;
        }
        RegionPos pos = RegionPos.from(player.getChunkPos());
        removeRegionFromPrunedData(source, pos);
        ((PrunedServerPlayerEntity) player).pruned$loadPrunedStatus(player, pos);
        source.getSource().sendFeedback(() -> Text.literal(String.format("Removed current region (%s) from Pruned world download", pos)), false);
        return 1;
    }

    private static void addRegionToPrunedData(CommandContext<ServerCommandSource> source, RegionPos pos) {
        Path path = PositionHelpers.regionPosToRegionFile(source.getSource().getServer(), source.getSource().getWorld().getRegistryKey(), pos);
        PrunedData.getServerState(source.getSource().getServer()).updateFile(path);
    }

    private static void removeRegionFromPrunedData(CommandContext<ServerCommandSource> source, RegionPos pos) {
        Path path = PositionHelpers.regionPosToRegionFile(source.getSource().getServer(), source.getSource().getWorld().getRegistryKey(), pos);
        PrunedData.getServerState(source.getSource().getServer()).removeFile(path);
        WorldUploader.removeFile(source.getSource().getServer(), path);
    }
}
