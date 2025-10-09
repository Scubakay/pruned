package com.scubakay.pruned.util;

import com.scubakay.pruned.domain.RegionChunkBounds;
import com.scubakay.pruned.domain.RegionPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.nio.file.Path;

public class PositionHelpers {
    public static Path regionPosToRegionFile(MinecraftServer server, RegistryKey<World> dimension, RegionPos pos) {
        Path regionPath;
        String regionFile = String.format("r.%d.%d.mca", pos.x, pos.z);
        String dimFolder = getDimensionFolder(dimension);
        if (dimFolder.isEmpty()) {
            regionPath = getSavePath(server).resolve("region").resolve(regionFile).normalize();
        } else {
            regionPath = getSavePath(server).resolve(dimFolder).resolve("region").resolve(regionFile).normalize();
        }
        return regionPath;
    }

    private static String getDimensionFolder(RegistryKey<World> dimension) {
        String path = dimension.getValue().getPath();
        if (path.equals("the_nether")) {
            return "DIM-1";
        } else if (path.equals("the_end")) {
            return "DIM1";
        }
        return "";
    }

    private static Path getSavePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT);
    }

    public static RegionChunkBounds getRegionChunkBounds(RegionPos pos) {
        ChunkPos start = new ChunkPos(pos.x * 32, pos.z * 32);
        ChunkPos end = new ChunkPos(pos.x * 32 + 31, pos.z * 32 + 31);
        return new RegionChunkBounds(start, end);
    }
}
