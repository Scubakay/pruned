package com.scubakay.pruned.mixin;

import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.data.ScoreboardManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@SuppressWarnings("UnusedMixin")
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Unique
    private int pruned$previousRegionX = Integer.MIN_VALUE;
    @Unique
    private int pruned$previousRegionZ = Integer.MIN_VALUE;

    @Inject(method = "tick", at = @At("HEAD"))
    private void pruned$onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        ChunkPos chunkPos = player.getChunkPos();
        int regionX = chunkPos.x / 32;
        int regionZ = chunkPos.z / 32;
        if (pruned$previousRegionX != regionX || pruned$previousRegionZ != regionZ) {
            pruned$previousRegionX = regionX;
            pruned$previousRegionZ = regionZ;
            if (!ScoreboardManager.getBooleanScore(player, ScoreboardManager.PRUNED_CHECK_SCOREBOARD)) {
                return;
            }

            // Get dimension folder
            ServerWorld world = player.getWorld();
            String dimFolder = "";
            Identifier dimId = world.getRegistryKey().getValue();
            if (dimId.getPath().equals("the_nether")) {
                dimFolder = "DIM-1";
            } else if (dimId.getPath().equals("the_end")) {
                dimFolder = "DIM1";
            }
            MinecraftServer server = world.getServer();
            Path savePath = server.getSavePath(WorldSavePath.ROOT);
            String regionFile = String.format("r.%d.%d.mca", regionX, regionZ);
            Path absoluteRegionPath;
            if (dimFolder.isEmpty()) {
                absoluteRegionPath = savePath.resolve("region").resolve(regionFile).normalize();
            } else {
                absoluteRegionPath = savePath.resolve(dimFolder).resolve("region").resolve(regionFile).normalize();
            }
            boolean inWorldDownload = PrunedData.getServerState(server).getFiles().containsKey(absoluteRegionPath);
            Text message;
            if (inWorldDownload) {
                message = Text.literal(String.format("Current region (%s, %s) is in the world download ", regionX, regionZ))
                    .append(Text.literal("[Remove]")
                        .styled(style -> style
                            .withClickEvent(new ClickEvent.RunCommand("/pruned remove"))
                            .withColor(Colors.RED)
                        )
                    );
            } else {
                message = Text.literal(String.format("Current region (%s, %s) is not in the world download ", regionX, regionZ))
                    .append(Text.literal("[Add]")
                        .styled(style -> style
                            .withClickEvent(new ClickEvent.RunCommand("/pruned save"))
                            .withColor(Colors.GREEN)
                        )
                    );
            }
            player.sendMessage(message, false);
        }
    }
}
