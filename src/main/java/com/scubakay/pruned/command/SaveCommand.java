package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.util.PositionHelpers;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.domain.RegionChunkBounds;
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
        setInhabitedTimeForCurrentChunk(source, Config.inhabitedTime * 20 * 60);
        addRegionToPrunedData(source, pos);
        ((PrunedServerPlayerEntity) player).pruned$loadPrunedStatus();
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
        setInhabitedTimeForRegion(source, PositionHelpers.getRegionChunkBounds(pos), 1);
        removeRegionFromPrunedData(source, pos);
        ((PrunedServerPlayerEntity)player).pruned$loadPrunedStatus();
        source.getSource().sendFeedback(() -> Text.literal(String.format("Removed current region (%s) from Pruned world download", pos)), false);
        return 1;
    }

    private static void addRegionToPrunedData(CommandContext<ServerCommandSource> source, RegionPos pos) {
        Path regionFile = PositionHelpers.regionPosToRegionFile(source.getSource().getServer(), source.getSource().getWorld().getRegistryKey(), pos);
        PrunedData.getServerState(source.getSource().getServer()).updateFile(regionFile);
    }

    private static void removeRegionFromPrunedData(CommandContext<ServerCommandSource> source, RegionPos pos) {
        Path regionFile = PositionHelpers.regionPosToRegionFile(source.getSource().getServer(), source.getSource().getWorld().getRegistryKey(), pos);
        PrunedData.getServerState(source.getSource().getServer()).removeRegion(regionFile);
    }

    private static void setInhabitedTimeForCurrentChunk(CommandContext<ServerCommandSource> source, int time) {
        String command = String.format("/inhabitor set ~ ~ ~ ~ %d", time);
        if (Config.debug) {
            source.getSource().sendFeedback(() -> Text.literal("[pruned debug] Executing: " + command), false);
        }
        ServerCommandSource src = source.getSource().withLevel(4); // Ensure sufficient permission
        src.getServer().getCommandManager().executeWithPrefix(src, command);
    }

    private static void setInhabitedTimeForRegion(CommandContext<ServerCommandSource> source, RegionChunkBounds bounds, int time) {
        int startBlockX = bounds.start().x * 16;
        int startBlockZ = bounds.start().z * 16;
        int endBlockX = bounds.end().x * 16 + 15;
        int endBlockZ = bounds.end().z * 16 + 15;
        String command = String.format("/inhabitor set %d %d %d %d %d", startBlockX, startBlockZ, endBlockX, endBlockZ, time);
        if (Config.debug) {
            source.getSource().sendFeedback(() -> Text.literal("[pruned debug] Executing: " + command), false);
        }
        ServerCommandSource src = source.getSource().withLevel(4); // Ensure sufficient permission
        src.getServer().getCommandManager().executeWithPrefix(src, command);
    }
}
