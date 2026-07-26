package dev.elysium.adventure.listener;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.dungeon.ActiveDungeon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class DungeonListener implements Listener {

    private final ElysiumAdventure plugin;

    public DungeonListener(ElysiumAdventure plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        ActiveDungeon dungeon = plugin.getDungeonManager().getDungeon(event.getEntity());
        if (dungeon == null) return;
        event.setDeathMessage(null); // An death message trong dungeon
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Neu chet trong dungeon -> respawn o spawn chinh
        if (plugin.getDungeonManager().inDungeon(event.getPlayer())) {
            plugin.getDungeonManager().leaveDungeon(event.getPlayer());
        }
    }
}
