package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PositionHelpers;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.data.RegionChunkBounds;
import com.scubakay.pruned.data.RegionPos;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;

import static com.scubakay.pruned.command.PermissionManager.*;

public class SaveCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> activateNode = CommandManager.literal("save")
                .requires(ctx -> hasPermission(ctx, SAVE_REGION_PERMISSION))
                .executes(SaveCommand::save)
                .build();
        Commands.getRoot(dispatcher).addChild(activateNode);

        LiteralCommandNode<ServerCommandSource> deactivateNode = CommandManager.literal("remove")
                .requires(ctx -> hasPermission(ctx, REMOVE_REGION_PERMISSION))
                .executes(SaveCommand::remove)
                .build();
        Commands.getRoot(dispatcher).addChild(deactivateNode);
    }

    private static int save(CommandContext<ServerCommandSource> source) {
        RegionPos pos = RegionPos.from(source.getSource().getPlayer().getChunkPos());
        setInhabitedTimeForRegion(source, PositionHelpers.getRegionChunkBounds(pos), Config.inhabitedTime * 20 * 60);
        addRegionToPrunedData(source, pos);
        source.getSource().sendFeedback(() -> Text.literal(String.format("Added current region (%s) to Pruned world download", pos)), false);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> source) {
        RegionPos pos = RegionPos.from(source.getSource().getPlayer().getChunkPos());
        setInhabitedTimeForRegion(source, PositionHelpers.getRegionChunkBounds(pos), 1);
        removeRegionFromPrunedData(source, pos);
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

    private static void setInhabitedTimeForRegion(CommandContext<ServerCommandSource> source, RegionChunkBounds bounds, int time) {
        String command = String.format("/inhabitor set %d %d %d %d %d", bounds.start().x, bounds.start().z, bounds.end().x, bounds.end().z, time);
        ServerCommandSource src = source.getSource().withLevel(4); // Ensure sufficient permission
        src.getServer().getCommandManager().executeWithPrefix(src, command);
    }
}
