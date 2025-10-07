package com.scubakay.pruned.mixin;

import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PositionHelpers;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.data.RegionPos;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.world.chunk.Chunk;

import java.nio.file.Path;

@SuppressWarnings("UnusedMixin")
@Mixin(ServerChunkLoadingManager.class)
public class ServerChunkLoadingManagerMixin {
    @Final
    @Shadow
    ServerWorld world;

    @Inject(
        method = "save(Lnet/minecraft/world/chunk/Chunk;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;visit(Ljava/lang/String;)V", shift = At.Shift.AFTER)
    )
    private void onChunkSave(Chunk chunk, CallbackInfoReturnable<Boolean> cir) {
        PrunedData pruned = PrunedData.getServerState();
        long minInhabitedTimeInTicks = Config.inhabitedTime * 20 * 60L;
        if (pruned != null && pruned.isActive()) {
            if (chunk.getInhabitedTime() >= minInhabitedTimeInTicks) {
                final Path path = PositionHelpers.regionPosToRegionFile(world.getServer(), world.getRegistryKey(), RegionPos.from(chunk.getPos()));
                PrunedData.getServerState().updateFile(path);
            }
        }
    }
}
