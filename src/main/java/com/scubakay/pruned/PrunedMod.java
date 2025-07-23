package com.scubakay.pruned;

import com.scubakay.pruned.command.PrunedCommand;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.PrunedData;
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
        ServerLifecycleEvents.SERVER_STARTED.register(PrunedData::setServer);
        CommandRegistrationCallback.EVENT.register(PrunedCommand::register);
    }
}