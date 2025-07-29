package com.scubakay.pruned.mixin.midnightlib;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.midnightdust.lib.config.AutoCommand;
import eu.midnightdust.lib.util.PlatformFunctions;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnusedMixin")
@Mixin(AutoCommand.class)
public class AutoCommandMixin {
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Leu/midnightdust/lib/util/PlatformFunctions;registerCommand(Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;)V"
        ),
        remap = false
    )
    private static void pruned$putConfigUnderModRoot(LiteralArgumentBuilder<ServerCommandSource> ignoredCommand, @Local LiteralArgumentBuilder<ServerCommandSource> command) {
        PlatformFunctions.registerCommand(command);
    }
}