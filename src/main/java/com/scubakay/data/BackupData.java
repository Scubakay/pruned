package com.scubakay.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.scubakay.PrunedMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BackupData extends PersistentState {
    private static MinecraftServer server;
    private final Map<String, Path> regions;

    public BackupData() {
        this(new HashMap<>());
    }

    public BackupData(Map<String, Path> regions) {
        this.regions = new HashMap<>(regions);
    }

    public Map<String, Path> getRegions() {
        return regions;
    }

    public void updateRegion(Path path) {
        this.regions.put(path.toString(), path);
        this.markDirty();
    }

    public static void setServer(MinecraftServer s) {
        server = s;
    }

    public static BackupData getServerState(MinecraftServer s) {
        setServer(s);
        return getServerState();
    }

    public static BackupData getServerState() {
        ServerWorld serverWorld = server.getWorld(World.OVERWORLD);
        assert serverWorld != null;
        BackupData state = getState(serverWorld);
        state.markDirty();
        return state;
    }

    public static final Codec<Path> PATH_CODEC = Codec.STRING.xmap(Path::of, Path::toString);

    public static final Codec<BackupData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PATH_CODEC).fieldOf("regions").forGetter(BackupData::getRegions)
    ).apply(instance, BackupData::new));

    public static PersistentStateType<BackupData> createStateType(String id) {
        return new PersistentStateType<>(PrunedMod.getSaveKey(id), BackupData::new, CODEC, null);
    }

    private static BackupData getState(ServerWorld serverWorld) {
        return serverWorld.getPersistentStateManager().getOrCreate(createStateType("backupData"));
    }
}
