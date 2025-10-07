package com.scubakay.pruned.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class Config extends MidnightConfig {
    // Inhabited time in minutes needed before chunk gets added to world download
    @Entry()
    public static int inhabitedTime = 60;
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

    // Automatically add chunks to the world download when the inhabited time increases to inhabitedTime
    @Entry()
    public static boolean autoSync = true;
    @Entry()
    public static int regionDebounceTime = 5;

    // TODO: REMOVE THE WEBDAV AUTH STUFF
    @Entry()
    public static String webDavEndpoint;
    @Entry()
    public static String webDavUsername;
    @Entry()
    public static String webDavPassword;
}
