package com.scubakay.pruned;

import com.scubakay.pruned.command.LoginCommand;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.data.BackupData;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrunedMod implements ModInitializer {
    public static final String MOD_ID = "pruned";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static String getSaveKey(String namespace) {
        return "%s_%s".formatted(MOD_ID, namespace);
    }

    @Override
    public void onInitialize() {
        MidnightConfig.init(MOD_ID, Config.class);
        ServerLifecycleEvents.SERVER_STARTED.register(BackupData::setServer);
        CommandRegistrationCallback.EVENT.register(LoginCommand::register);
    }
}