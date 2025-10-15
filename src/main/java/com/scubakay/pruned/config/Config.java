package com.scubakay.pruned.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class Config extends MidnightConfig {
    @Entry() public static UploadStrategy uploadStrategy = UploadStrategy.MANUAL;
    @Entry() public static int uploadInterval = 15; // In minutes
    @Entry() public static boolean stopUploadOnServerStop = true;

    @Entry() public static boolean autoAddInhabitedChunks = true;
    @Entry() public static int inhabitedTime = 60;

    @Entry() public static String uploadFolder = "Pruned";
    @Entry() public static int maxConcurrentUploads = 4;
    @Entry() public static boolean debug = false;

    @Hidden @Entry()
    public static List<String> ignored = Lists.newArrayList(
            "pruned.dat",
            ".mca",
            ".sqlite*",
            ".dat_old",
            ".lock"
    );

    @Hidden @Entry() public static int permissionLevel = 4;
    @Hidden @Entry() public static String webDavEndpoint;
    @Hidden @Entry() public static String webDavUsername;
    @Hidden @Entry() public static String webDavPassword;

    public enum UploadStrategy {
        INTERVAL,
        SERVER_STOP,
        MANUAL
    }
}
