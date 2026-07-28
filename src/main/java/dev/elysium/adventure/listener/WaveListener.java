package dev.elysium.adventure.listener;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.wave.ActiveWaveDungeon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WaveListener implements Listener {

    private final ElysiumAdventure plugin;

    public WaveListener(ElysiumAdventure plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) return;

        // Kiem tra entity co trong wave dungeon nao khong
        ActiveWaveDungeon dungeon = plugin.getWaveManager().getDungeonByWorld(entity.getWorld());
        if (dungeon == null) return;

        // Xoa drop mac dinh (wave dungeon co reward rieng)
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Thong bao WaveManager
        plugin.getWaveManager().onMobDeath(entity.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getWaveManager().inDungeon(player)) {
            plugin.getWaveManager().onPlayerQuit(player);
        }
    }
}
