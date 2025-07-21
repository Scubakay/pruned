package com.scubakay.config;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;

import java.util.List;

public class Config extends MidnightConfig {
    @Entry()
    public static int inhabitedTime = 50;
    @Entry()
    public static List<String> ignored = Lists.newArrayList(
            "prunedworlddownload_backupData.dat",
            ".mca",
            ".sqlite*",
            ".dat_old",
            ".lock"
    );
    @Entry()
    public static boolean debug = false;
}
