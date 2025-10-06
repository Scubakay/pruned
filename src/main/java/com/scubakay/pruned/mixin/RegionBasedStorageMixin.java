package com.scubakay.pruned.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.RegionBasedStorage;
import net.minecraft.world.storage.RegionFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@SuppressWarnings("UnusedMixin")
@Mixin(RegionBasedStorage.class)
public class RegionBasedStorageMixin {
	/**
	 * Todo: Remove region files from world download update when region file is deleted?
	 */
	@Inject(
			method = "write",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/storage/RegionFile;getChunkOutputStream(Lnet/minecraft/util/math/ChunkPos;)Ljava/io/DataOutputStream;"
			))
	private void pruned$injectRegionFileRegistration(ChunkPos pos, NbtCompound nbt, CallbackInfo ci, @Local RegionFile regionFile) {
		PrunedData pruned = PrunedData.getServerState();
		if (pruned != null && pruned.isActive()) {
			if (nbt.getLong("InhabitedTime").orElse(0L) >= Config.inhabitedTime) {
				final Path path = regionFile.getPath();
				PrunedData.getServerState().updateFile(path);
			}
		}
	}
}