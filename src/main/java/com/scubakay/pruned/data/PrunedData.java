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

    // These are the files uploaded to WebDAV with local path as key and sha1 as value
    private final Map<Path, String> files;
    private boolean active;
    private String resourcePackHash;

    public Map<Path, String> getFiles() {
        return files;
    }

    public String getResourcePackHash() {
        return resourcePackHash;
    }

    public void setResourcePackHash(String hash) {
        resourcePackHash = hash;
        this.markDirty();
    }

    public boolean isActive() {
        return active;
    }

    public PrunedData() {
        this(false, new HashMap<>(), "");
    }

    public PrunedData(boolean isActive, Map<Path, String> regions, String hash) {
        this.files = new HashMap<>(regions);
        this.active = isActive;
        this.resourcePackHash = hash;
    }

    public void activate() {
        active = true;
        this.markDirty();
    }

    public void deactivate() {
        active = false;
        this.markDirty();
    }

    public void addFile(Path path) {
        this.updateSha1(path, "");
    }

    public String getSha1(Path path) {
        return this.files.get(path);
    }

    public void updateSha1(Path path, String sha1) {
        this.files.put(path, sha1);
        this.markDirty();
    }

    public void updateFile(Path path) {
        this.files.put(path.normalize(), "");
        this.markDirty();
    }

    public void removeFile(Path path) {
        this.files.remove(path);
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
            Codec.unboundedMap(PATH_CODEC, Codec.STRING).fieldOf("files").forGetter(PrunedData::getFiles),
            Codec.STRING.fieldOf("resourcePackHash").forGetter(PrunedData::getResourcePackHash)
    ).apply(instance, PrunedData::new));

    private static PrunedData getState(ServerWorld serverWorld) {
        return serverWorld.getPersistentStateManager()
                .getOrCreate(new PersistentStateType<>("pruned", PrunedData::new, CODEC, null));
    }
}
