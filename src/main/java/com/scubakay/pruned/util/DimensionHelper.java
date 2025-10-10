package com.scubakay.pruned.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class DimensionHelper {
    public static String getFormattedDimension(RegistryKey<World> dimensionKey) {
        String dimensionId = dimensionKey.getValue().toString();
        return switch (dimensionId) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> dimensionId;
        };
    }

}
