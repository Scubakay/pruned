package com.scubakay.pruned;

import com.scubakay.pruned.command.PrunedCommand;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
import com.scubakay.pruned.storage.WorldUploader;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Entrypoint
public class PrunedMod implements ModInitializer {
    public static final String MOD_ID = "pruned";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ScheduledExecutorService scheduler;

    @Override
    public void onInitialize() {
        MidnightConfig.init(MOD_ID, Config.class);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            PrunedData.setServer(server);
            Path path = server.getSavePath(WorldSavePath.ROOT);
            scheduler = Executors.newScheduledThreadPool(2);

            if (Config.debug) {
                LOGGER.info("Uploading updated regions every {} seconds", Config.regionSyncInterval);
            }

            // Schedule region sync
            scheduler.scheduleAtFixedRate(
                () -> WorldUploader.synchronizeDirty(path, PrunedData.getServerState(server).getRegions()),
                Config.regionSyncInterval, // initial delay
                Config.regionSyncInterval, // period
                TimeUnit.SECONDS
            );

            if (Config.debug) {
                LOGGER.info("Uploading non-region world files every {} seconds", Config.worldSyncInterval);
            }

            // Schedule world sync
            scheduler.scheduleAtFixedRate(
                () -> WorldUploader.synchronizeWithIgnoreList(path),
                Config.worldSyncInterval, // initial delay
                Config.worldSyncInterval, // period
                TimeUnit.SECONDS
            );
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown(); // Allow running tasks to finish
            }
            if (Config.debug) {
                LOGGER.info("Stopping world uploads. Currently running uploads will continue.");
            }
        });
        CommandRegistrationCallback.EVENT.register(PrunedCommand::register);
    }
}