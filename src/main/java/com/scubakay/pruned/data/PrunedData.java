package com.scubakay.pruned.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.PersistentStateType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PrunedData extends PersistentState {
    private static MinecraftServer server;
    private final Map<String, Path> regions;
    private boolean active;

    public Map<String, Path> getRegions() {
        return regions;
    }

    public boolean isActive() {
        return active;
    }

    public PrunedData() {
        this(false, new HashMap<>());
    }

    public PrunedData(boolean isActive, Map<String, Path> regions) {
        this.regions = new HashMap<>(regions);
        this.active = isActive;
    }

    public void activate() {
        active = true;
        this.markDirty();
    }

    public void deactivate() {
        active = false;
        this.markDirty();
    }

    public void updateRegion(Path path) {
        this.regions.put(path.toString(), path);
        this.markDirty();
    }

    public static void setServer(MinecraftServer s) {
        server = s;
    }

    public static PrunedData getServerState(MinecraftServer s) {
        setServer(s);
        return getServerState();
    }

    public static PrunedData getServerState() {
        if (server == null) return null;
        ServerWorld serverWorld = server.getWorld(World.OVERWORLD);
        assert serverWorld != null;
        PrunedData state = getState(serverWorld);
        state.markDirty();
        return state;
    }

    public static final Codec<Path> PATH_CODEC = Codec.STRING.xmap(Path::of, Path::toString);

    public static final Codec<PrunedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("active").forGetter(PrunedData::isActive),
            Codec.unboundedMap(Codec.STRING, PATH_CODEC).fieldOf("regions").forGetter(PrunedData::getRegions)
    ).apply(instance, PrunedData::new));

    private static PrunedData getState(ServerWorld serverWorld) {
        return serverWorld.getPersistentStateManager()
                .getOrCreate(new PersistentStateType<>("pruned", PrunedData::new, CODEC, null));
    }
}
