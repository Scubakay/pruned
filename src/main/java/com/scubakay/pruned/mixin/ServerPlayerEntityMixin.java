package com.scubakay.pruned.mixin;

import com.scubakay.pruned.data.PositionHelpers;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.data.RegionPos;
import com.scubakay.pruned.domain.PrunedServerPlayerEntity;
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
public class ServerPlayerEntityMixin implements PrunedServerPlayerEntity {
    @Unique
    private RegionPos previousChunk = new RegionPos(Integer.MIN_VALUE, Integer.MIN_VALUE);

    @Unique
    private boolean regionInWorldDownload;

    @Override
    public boolean pruned$isRegionSaved() {
        return regionInWorldDownload;
    }

    @Override
    public void pruned$setIsRegionSaved(boolean value) {
        this.regionInWorldDownload = value;
    }

    @Unique
    private boolean regionHelperEnabled;

    @Override
    public boolean pruned$isRegionHelperEnabled() {
        return regionHelperEnabled;
    }

    @Override
    public void pruned$toggleRegionHelper() {
        this.regionHelperEnabled = !this.regionHelperEnabled;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void pruned$onTick(CallbackInfo ci) {
        pruned$loadPrunedStatus();
    }

    public void pruned$loadPrunedStatus() {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        RegionPos pos = RegionPos.from(player.getChunkPos());
        if (!previousChunk.equals(pos)) {
            previousChunk = pos;

            MinecraftServer server = player.getServer();
            Path regionFile = PositionHelpers.regionPosToRegionFile(server, player.getWorld().getRegistryKey(), pos);
            regionInWorldDownload = PrunedData.getServerState(server).getFiles().containsKey(regionFile);

            if (regionHelperEnabled) {
                Text message = getHelperMessage(
                            String.format("Current region (%s) is %sin the world download ", pos.toString(), regionInWorldDownload ? "" : "not"),
                            regionInWorldDownload ? "Remove" : "[Add]",
                            regionInWorldDownload ? "/pruned remove" : "/pruned save",
                            regionInWorldDownload ? Colors.RED : Colors.GREEN
                    );
                player.sendMessage(message, false);
            }
        }
    }

    @Unique
    private static Text getHelperMessage(String message, String button, String command, int color) {
        return Text.literal(message)
                .append(Text.literal(button)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.RunCommand(command))
                                .withColor(color)
                        )
                );
    }
}
