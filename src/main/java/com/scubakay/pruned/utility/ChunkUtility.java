package com.scubakay.pruned.utility;

import java.nio.file.Path;

public class ChunkUtility {
    public static Path chunkCoordToRegionFile(int chunkX, int chunkZ) {
        int regionX = chunkX / 32;
        int regionZ = chunkZ / 32;
        return Path.of(String.format("r.%d.%d.mca", regionX, regionZ));
    }
}
