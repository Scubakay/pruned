package com.scubakay.pruned.dialog;

import com.mojang.brigadier.context.CommandContext;
import com.scubakay.pruned.config.Config;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Map;
import java.util.Objects;

public class WebDavConfigDialog {
    public static int openWebDavConfigDialog(CommandContext<ServerCommandSource> context) {
        Objects.requireNonNull(DynamicDialog.create("webdav_config")).show(context, Map.of(
            "%ENDPOINT%", Config.webDavEndpoint != null ? Config.webDavEndpoint : "",
            "%USERNAME%", Config.webDavUsername != null ? Config.webDavUsername : ""
        ));
        return 1;
    }
}
