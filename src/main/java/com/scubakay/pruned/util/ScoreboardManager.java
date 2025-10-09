package com.scubakay.pruned.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ScoreboardManager {
    public final static String PRUNED_CHECK_SCOREBOARD = "pruned_check";
    public final static String PRUNED_CURRENT_REGION_IS_SAVED = "pruned_current_region_is_saved";

    public static void createScoreboard(MinecraftServer server, String scoreboardName, String displayName) {
        Scoreboard scoreboard = server.getScoreboard();
        if (!scoreboard.getObjectiveNames().contains(scoreboardName)) {
            scoreboard.addObjective(
                    scoreboardName,
                    ScoreboardCriterion.DUMMY,
                    Text.literal(displayName),
                    ScoreboardCriterion.RenderType.INTEGER,
                    false, // displayAutoUpdate
                    null   // numberFormat
            );
        }
    }

    public static boolean getBooleanScore(ServerPlayerEntity player, String scoreboardName) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(scoreboardName);
        ScoreAccess scoreAccess = scoreboard.getOrCreateScore(player, objective);
        return scoreAccess.getScore() == 1;
    }

    public static boolean toggleBooleanScore(ServerPlayerEntity player, String scoreboardName) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(scoreboardName);
        ScoreAccess scoreAccess = scoreboard.getOrCreateScore(player, objective);

        int newScore = scoreAccess.getScore() == 1 ? 0 : 1;
        scoreAccess.setScore(newScore);
        return newScore == 1;
    }

    public static void setBooleanScore(ServerPlayerEntity player, String scoreboardName, boolean score) {
        Scoreboard scoreboard = player.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective(scoreboardName);
        ScoreAccess scoreAccess = scoreboard.getOrCreateScore(player, objective);
        scoreAccess.setScore(score ? 1 : 0);
    }
}
