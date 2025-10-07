package com.scubakay.pruned.mixin;

import com.scubakay.pruned.data.PositionHelpers;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.data.RegionPos;
import com.scubakay.pruned.data.ScoreboardManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
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
    private RegionPos previousChunk = new RegionPos(Integer.MIN_VALUE, Integer.MIN_VALUE);

    @Inject(method = "tick", at = @At("HEAD"))
    private void pruned$onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        RegionPos pos = RegionPos.from(player.getChunkPos());
        if (!previousChunk.equals(pos)) {
            previousChunk = pos;

            MinecraftServer server = player.getServer();
            Path regionFile = PositionHelpers.regionPosToRegionFile(server, player.getWorld().getRegistryKey(), pos);
            boolean inWorldDownload = PrunedData.getServerState(server).getFiles().containsKey(regionFile);

            ScoreboardManager.setBooleanScore(player, ScoreboardManager.PRUNED_CURRENT_REGION_IS_SAVED, inWorldDownload);
            if (ScoreboardManager.getBooleanScore(player, ScoreboardManager.PRUNED_CHECK_SCOREBOARD)) {
                Text message;
                if (inWorldDownload) {
                    message = Text.literal(String.format("Current region (%s) is in the world download ", pos.toString()))
                            .append(Text.literal("[Remove]")
                                    .styled(style -> style
                                            .withClickEvent(new ClickEvent.RunCommand("/pruned remove"))
                                            .withColor(Colors.RED)
                                    )
                            );
                } else {
                    message = Text.literal(String.format("Current region (%s) is not in the world download ", pos.toString()))
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
}
