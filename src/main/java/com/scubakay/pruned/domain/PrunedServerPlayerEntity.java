package com.scubakay.pruned.domain;

import net.minecraft.server.network.ServerPlayerEntity;

public interface PrunedServerPlayerEntity {
    boolean pruned$isRegionSaved();
    void pruned$setIsRegionSaved(boolean value);
    boolean pruned$isRegionHelperEnabled();
    void pruned$toggleRegionHelper();
    void pruned$loadPrunedStatus(ServerPlayerEntity player, RegionPos pos);
}
