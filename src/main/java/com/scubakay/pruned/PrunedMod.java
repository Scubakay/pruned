package com.scubakay.pruned;

import com.scubakay.pruned.command.Commands;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.storage.WorldUploader;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entrypoint
public class PrunedMod implements ModInitializer {
    public static final String MOD_ID = "pruned";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        MidnightConfig.init(MOD_ID, Config.class);
        ServerLifecycleEvents.AFTER_SAVE.register(WorldUploader::afterSave);
        CommandRegistrationCallback.EVENT.register(Commands::register);
    }
}