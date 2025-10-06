package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.data.PrunedData;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

public class SaveCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> activateNode = CommandManager.literal("save")
                .executes(SaveCommand::save)
                .build();
        Commands.getRoot(dispatcher).addChild(activateNode);

        LiteralCommandNode<ServerCommandSource> deactivateNode = CommandManager.literal("remove")
                .executes(SaveCommand::remove)
                .build();
        Commands.getRoot(dispatcher).addChild(deactivateNode);
    }

    private static int save(CommandContext<ServerCommandSource> source) {
        ChunkPos pos = source.getSource().getPlayer().getChunkPos();
        int regionX = pos.x / 32;
        int regionZ = pos.z / 32;
        int x1 = regionX * 32;
        int z1 = regionZ * 32;
        int x2 = x1 + 31;
        int z2 = z1 + 31;
        int time = 99999;
        String command = String.format("/inhabitor set %d %d %d %d %d", x1, z1, x2, z2, time);
        ServerCommandSource src = source.getSource().withLevel(4); // Ensure sufficient permission
        src.getServer().getCommandManager().executeWithPrefix(src, command);
        source.getSource().sendFeedback(() -> Text.literal(String.format("Added current region (chunk %d, %d to chunk %d, %d) to Pruned world download", x1, z1, x2, z2)), false);
        return 1;
    }

    private static int remove(CommandContext<ServerCommandSource> source) {
        ChunkPos pos = source.getSource().getPlayer().getChunkPos();
        int regionX = pos.x / 32;
        int regionZ = pos.z / 32;
        int x1 = regionX * 32;
        int z1 = regionZ * 32;
        int x2 = x1 + 31;
        int z2 = z1 + 31;
        int time = 1;
        String command = String.format("/inhabitor set %d %d %d %d %d", x1, z1, x2, z2, time);
        ServerCommandSource src = source.getSource().withLevel(4); // Ensure sufficient permission
        src.getServer().getCommandManager().executeWithPrefix(src, command);
        String regionFileName = String.format("r.%d.%d.mca", regionX, regionZ);
        RegistryEntry<DimensionType> dimension = source.getSource().getWorld().getDimensionEntry();
        PrunedData.getServerState(source.getSource().getServer()).removeRegion(dimension, regionFileName);
        source.getSource().sendFeedback(() -> Text.literal(String.format("Removed current region (chunk %d, %d to chunk %d, %d) from Pruned world download", x1, z1, x2, z2)), false);
        return 1;
    }
}
