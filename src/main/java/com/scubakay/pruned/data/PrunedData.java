package com.scubakay.pruned.data;

import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.storage.WorldUploader;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.dimension.DimensionType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PrunedData extends PersistentState {
    private static MinecraftServer server;

    // These are the files uploaded to WebDAV with local path as key and sha1 as value
    private final Map<Path, String> files;
    private boolean active;

    public Map<Path, String> getFiles() {
        return files;
    }

    public boolean isActive() {
        return active;
    }

    public PrunedData() {
        this(false, new HashMap<>());
    }

    public PrunedData(boolean isActive, Map<Path, String> regions) {
        this.files = new HashMap<>(regions);
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

    // Debounce map: file path -> last update timestamp (ms)
    private static final ConcurrentHashMap<Path, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_INTERVAL_MS = Config.regionDebounceTime * 60_000L; // Per minute

    public void updateFile(Path path) {
        String sha1 = getSha1(path);
        if (sha1 == null) {
            if (Config.debug) PrunedMod.LOGGER.info("Could not get sha1 for {}", path);
            return;
        }
        if (!this.files.containsKey(path) || !this.files.get(path).equals(sha1)) {
            // Debounce after the file has been uploaded. Don't upload for a minute or so
            long now = System.currentTimeMillis();
            Long lastUpdate = lastUpdateTimes.get(path);
            if (lastUpdate != null && (now - lastUpdate) < DEBOUNCE_INTERVAL_MS) {
                return;
            }
            lastUpdateTimes.put(path, now);

            if (Config.debug) PrunedMod.LOGGER.info("Scheduling {} for upload", path);
            this.files.put(path, sha1);
            this.markDirty();
            WorldUploader.uploadFile(server, path);
        } else {
            if (Config.debug) PrunedMod.LOGGER.info("File {} is already up to date", path);
        }
    }

    public void removeRegion(RegistryEntry<DimensionType> dimension, String filename) {
        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        String dimFolder = "";
        if (dimension == World.OVERWORLD) {
            dimFolder = "";
        } else if (dimension == World.NETHER) {
            dimFolder = "DIM-1";
        } else if (dimension == World.END) {
            dimFolder = "DIM1";
        }
        Path regionFilePath = dimFolder.isEmpty()
            ? savePath.resolve("region").resolve(filename)
            : savePath.resolve(dimFolder).resolve("region").resolve(filename);
        // Remove from files map and call WorldUploader.removeFile
        this.files.remove(regionFilePath);
        WorldUploader.removeFile(server, regionFilePath);
        this.markDirty();
    }

    private String getSha1(Path path) {
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] hashBytes = MessageDigest.getInstance("SHA-1").digest(fileBytes);
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            return null;
        }
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
            Codec.unboundedMap(PATH_CODEC, Codec.STRING).fieldOf("files").forGetter(PrunedData::getFiles)
    ).apply(instance, PrunedData::new));

    private static PrunedData getState(ServerWorld serverWorld) {
        return serverWorld.getPersistentStateManager()
                .getOrCreate(new PersistentStateType<>("pruned", PrunedData::new, CODEC, null));
    }
}
