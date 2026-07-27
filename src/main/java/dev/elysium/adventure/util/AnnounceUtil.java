package dev.elysium.adventure.util;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class AnnounceUtil {

    private AnnounceUtil() {}

    // ── Title ─────────────────────────────────────────────────────────────────

    public static void title(Player player, String title, String subtitle) {
        title(player, title, subtitle, 10, 40, 10);
    }

    public static void title(Player player, String title, String subtitle,
                             int fadeIn, int stay, int fadeOut) {
        player.sendTitle(color(title), color(subtitle), fadeIn, stay, fadeOut);
    }

    public static void titleAll(Collection<? extends Player> players, String title, String subtitle) {
        for (Player p : players) title(p, title, subtitle);
    }

    // ── Sound ─────────────────────────────────────────────────────────────────

    public static void sound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    public static void sound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void soundAll(Collection<? extends Player> players, Sound sound) {
        for (Player p : players) sound(p, sound);
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────

    public static void broadcast(String message) {
        Bukkit.broadcastMessage(color("&5&l[Adventure] &r" + message));
    }

    public static void broadcastParty(Collection<? extends Player> players, String message) {
        for (Player p : players) p.sendMessage(color("&5[Party] &r" + message));
    }

    // ── Dungeon Announces ─────────────────────────────────────────────────────

    public static void dungeonEnter(Collection<? extends Player> players, String dungeonName) {
        for (Player p : players) {
            title(p, "&5&l⚔ DUNGEON", "&f" + dungeonName, 10, 50, 10);
            sound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
            p.sendMessage(color("&5&l[Dungeon] &fBan da vao &5" + dungeonName + "&f!"));
        }
    }

    public static void dungeonComplete(Collection<? extends Player> players, String dungeonName, String timeStr) {
        for (Player p : players) {
            title(p, "&a&l✔ HOAN THANH!", "&f" + dungeonName + " &7(" + timeStr + ")", 10, 70, 20);
            sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.sendMessage(color("&a&l[Dungeon] &fHoan thanh &a" + dungeonName
                    + " &ftrong &e" + timeStr + "&f!"));
        }
    }

    public static void dungeonFail(Collection<? extends Player> players, String reason) {
        for (Player p : players) {
            title(p, "&c&l✘ THAT BAI", "&f" + reason, 10, 50, 10);
            sound(p, Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
            p.sendMessage(color("&c&l[Dungeon] &fThat bai: " + reason));
        }
    }

    // ── Boss Announces ────────────────────────────────────────────────────────

    public static void bossPhaseChange(Collection<? extends Player> players,
                                       String bossName, String phaseName, String announce) {
        for (Player p : players) {
            title(p, "&c" + bossName, "&f" + phaseName, 5, 40, 10);
            sound(p, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.7f);
            if (announce != null) p.sendMessage(color(announce));
        }
    }

    public static void bossDeath(String bossName) {
        broadcast("&f" + bossName + " &ada bi tieu diet!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            sound(p, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1f);
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    public static String formatTime(long seconds) {
        if (seconds >= 3600) {
            return String.format("%dh %dm %ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        }
        if (seconds >= 60) return String.format("%dm %ds", seconds / 60, seconds % 60);
        return seconds + "s";
    }

    private static String color(String s) {
        return s.replace("&", "\u00a7");
    }
}
