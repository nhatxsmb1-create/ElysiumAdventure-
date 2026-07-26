package dev.elysium.adventure.api;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.boss.ActiveBoss;
import dev.elysium.adventure.dungeon.ActiveDungeon;
import dev.elysium.adventure.party.Party;
import org.bukkit.entity.Player;

/**
 * Public API cho cac plugin khac (ElysiumWar, ElysiumSeason...) su dung.
 *
 * Vi du:
 *   Party party = AdventureAPI.getParty(player);
 *   boolean inDungeon = AdventureAPI.inDungeon(player);
 */
public class AdventureAPI {

    private static ElysiumAdventure plugin;

    public static void init(ElysiumAdventure instance) { plugin = instance; }

    // ── Party ─────────────────────────────────────────────────────────────────

    public static Party   getParty(Player player)  { return plugin.getPartyManager().getParty(player); }
    public static boolean inParty(Player player)   { return plugin.getPartyManager().inParty(player); }

    // ── Dungeon ───────────────────────────────────────────────────────────────

    public static ActiveDungeon getDungeon(Player player) { return plugin.getDungeonManager().getDungeon(player); }
    public static boolean       inDungeon(Player player)  { return plugin.getDungeonManager().inDungeon(player); }

    // ── Boss ──────────────────────────────────────────────────────────────────

    public static ActiveBoss spawnBoss(String bossId, org.bukkit.Location loc) {
        return plugin.getBossManager().spawnBoss(bossId, loc);
    }
    public static boolean isBoss(java.util.UUID entityUuid) {
        return plugin.getBossManager().isBoss(entityUuid);
    }
}
