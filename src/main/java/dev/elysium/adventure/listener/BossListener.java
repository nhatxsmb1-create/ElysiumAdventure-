package dev.elysium.adventure.listener;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.boss.ActiveBoss;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class BossListener implements Listener {

    private final ElysiumAdventure plugin;

    public BossListener(ElysiumAdventure plugin) { this.plugin = plugin; }

    /** Chan damage mac dinh cua Bukkit voi boss — ElysiumAdventure tu xu ly */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (!plugin.getBossManager().isBoss(entity.getUniqueId())) return;

        double damage = event.getFinalDamage();
        // Lay UUID nguoi tan cong
        java.util.UUID damagerUuid = null;
        if (event instanceof EntityDamageByEntityEvent edbe) {
            if (edbe.getDamager() instanceof Player p) damagerUuid = p.getUniqueId();
        }

        // Cancel event goc, tu xu ly HP
        event.setCancelled(true);
        if (damagerUuid != null && damage > 0) {
            plugin.getBossManager().handleDamage(entity.getUniqueId(), damage, damagerUuid);

            // Combat EXP nho moi hit boss (x1.5 vi danh boss)
            org.bukkit.entity.Player damager = org.bukkit.Bukkit.getPlayer(damagerUuid);
            if (damager != null) giveWeaponCombatExp(damager, "BOSS");
        }
    }

    private void giveWeaponCombatExp(org.bukkit.entity.Player player, String source) {
        try {
            Class<?> api = Class.forName("dev.elysium.weapon.api.WeaponAPI");
            String weaponId = (String) api.getMethod("getHeldWeaponId", org.bukkit.entity.Player.class)
                    .invoke(null, player);
            if (weaponId != null) {
                // 2 EXP moi hit boss (x1.5 multiplier trong WeaponMastery)
                api.getMethod("addWeaponExp", org.bukkit.entity.Player.class, String.class, long.class, String.class)
                        .invoke(null, player, weaponId, 2L, source);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception ignored) {}
    }

    /** Bo qua entity death event mac dinh voi boss (da xu ly trong handleDamage) */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (plugin.getBossManager().isBoss(event.getEntity().getUniqueId())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    /** Goi DungeonManager khi boss chet */
    @EventHandler
    public void onBossDeath(dev.elysium.adventure.event.BossDeathEvent event) {
        plugin.getDungeonManager().onBossDeath(event.getBoss());
    }
}
