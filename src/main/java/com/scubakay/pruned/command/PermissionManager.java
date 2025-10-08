package com.scubakay.pruned.command;

import com.scubakay.pruned.config.Config;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionManager {
    public static final String ROOT_PERMISSION = "pruned";

    public static final String REGION_PERMISSION = ROOT_PERMISSION + ".manage";
    public static final String SAVE_REGION_PERMISSION = REGION_PERMISSION + ".save";
    public static final String REMOVE_REGION_PERMISSION = REGION_PERMISSION + ".remove";

    public static final String CONFIGURE_PERMISSION = ROOT_PERMISSION + ".configure";

    private static final boolean fabricPermissionsApi = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    public static boolean hasPermission(ServerPlayerEntity player, String permission) {
        if (player.hasPermissionLevel(Config.permissionLevel)) return true;
        return fabricPermissionsApi && Permissions.check(player, permission);
    }

    public static boolean hasPermission(ServerCommandSource source, String permission) {
        if (source.hasPermissionLevel(Config.permissionLevel)) return true;
        return fabricPermissionsApi && Permissions.check(source, permission);
    }
}
