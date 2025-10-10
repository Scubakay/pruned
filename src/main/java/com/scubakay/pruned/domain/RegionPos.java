package com.scubakay.pruned.domain;

import net.minecraft.util.math.ChunkPos;

public class RegionPos {
    public int x;
    public int z;

    public RegionPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionPos other)) return false;
        return x == other.x && z == other.z;
    }

    @Override
    public int hashCode() {
        return 31 * x + z;
    }

    @Override
    public String toString() {
        return x + ", " + z;
    }

    public static RegionPos from(ChunkPos pos) {
        int regionX = Math.floorDiv(pos.x, 32);
        int regionZ = Math.floorDiv(pos.z, 32);
        return new RegionPos(regionX, regionZ);
    }
}