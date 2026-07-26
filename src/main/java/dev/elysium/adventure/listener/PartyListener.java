package dev.elysium.adventure.listener;

import dev.elysium.adventure.ElysiumAdventure;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PartyListener implements Listener {

    private final ElysiumAdventure plugin;

    public PartyListener(ElysiumAdventure plugin) { this.plugin = plugin; }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Tu dong roi party khi disconnect
        plugin.getPartyManager().leaveParty(event.getPlayer());
        // Tu dong roi dungeon khi disconnect
        if (plugin.getDungeonManager().inDungeon(event.getPlayer())) {
            plugin.getDungeonManager().leaveDungeon(event.getPlayer());
        }
    }
}
