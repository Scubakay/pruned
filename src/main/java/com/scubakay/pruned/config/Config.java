package com.scubakay.pruned.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class Config extends MidnightConfig {
    @Entry()
    public static int inhabitedTime = 50;
    @Entry()
    public static List<String> ignored = Lists.newArrayList(
            "pruned.dat",
            ".mca",
            ".sqlite*",
            ".dat_old",
            ".lock"
    );
    @Entry()
    public static boolean debug = false;

    @Entry()
    public static boolean autoSync = true;
    @Entry()
    public static int regionSyncInterval = 60; // every minute
    @Entry()
    public static int worldSyncInterval = 900; // every 15 minutes

    // TODO: REMOVE THE WEBDAV AUTH STUFF
    @Entry()
    public static String webDavEndpoint;
    @Entry()
    public static String webDavUsername;
    @Entry()
    public static String webDavPassword;
}
