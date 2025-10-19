package com.scubakay.pruned.dialog;

import com.mojang.brigadier.context.CommandContext;
import com.scubakay.pruned.command.PermissionManager;
import com.scubakay.pruned.domain.PrunedServerPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Map;

public class PrunedDialog {
    public static void openPrunedDialog(CommandContext<ServerCommandSource> context, PrunedServerPlayerEntity player) {
        DynamicDialog prunedDialog = DynamicDialog.create("pruned");
        if (prunedDialog == null) return;
        if (PermissionManager.hasPermission(context.getSource(), PermissionManager.UPLOAD_PERMISSION)) {
            prunedDialog.addDialogAction("upload_button");
        }
        if (PermissionManager.hasPermission(context.getSource(), PermissionManager.CONFIGURE_PERMISSION)) {
            prunedDialog.addDialogAction("webdav_config_button");
        }

        prunedDialog.show(context, Map.of(
            "%REGION_IN_WORLD_DOWNLOAD%", player.pruned$isRegionSaved() ? "Yes" : "No",
            "%REGION_IN_WORLD_DOWNLOAD_COLOR%", player.pruned$isRegionSaved() ? "green" : "yellow",
            "%ADD_OR_REMOVE%", player.pruned$isRegionSaved() ? "Remove" : "Add",
            "%ADD_OR_REMOVE_COLOR%", player.pruned$isRegionSaved() ? "red" : "green",
            "%ADD_OR_REMOVE_TOOLTIP%", player.pruned$isRegionSaved() ? "Remove the current region from the world download" : "Add the current region to the world download",
            "%PRUNED_SAVE_OR_REMOVE_COMMAND%", player.pruned$isRegionSaved() ? "pruned remove" : "pruned save",
            "%ENABLE_DISABLE_HELPER%", player.pruned$isRegionHelperEnabled() ? "Disable Helper" : "Enable Helper",
            "%ENABLE_DISABLE_HELPER_COLOR%", player.pruned$isRegionHelperEnabled() ? "red" : "green"
        ));
    }
}
