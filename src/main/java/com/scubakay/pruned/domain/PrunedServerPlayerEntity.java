package com.scubakay.pruned.domain;

public interface PrunedServerPlayerEntity {
    boolean pruned$isRegionSaved();
    void pruned$setIsRegionSaved(boolean value);
    boolean pruned$isRegionHelperEnabled();
    void pruned$toggleRegionHelper();
    void pruned$loadPrunedStatus();
}
