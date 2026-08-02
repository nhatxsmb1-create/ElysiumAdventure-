package dev.elysium.adventure.boss;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.event.BossDeathEvent;
import dev.elysium.adventure.event.BossPhaseChangeEvent;
import dev.elysium.adventure.loot.LootTable;
import dev.elysium.adventure.util.AnnounceUtil;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class BossManager {

    private final ElysiumAdventure plugin;
    private final BossSkillExecutor executor;

    private final Map<String, BossData>      bossDataMap  = new HashMap<>();
    private final Map<String, BossSkillData> skillDataMap = new HashMap<>();
    private final Map<UUID, ActiveBoss>      activeBosses = new HashMap<>();

    // BossBar: entity UUID -> BossBar
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    private BukkitTask aiTask;

    public BossManager(ElysiumAdventure plugin) {
        this.plugin   = plugin;
        this.executor = new BossSkillExecutor(plugin);
        loadConfig();
        startAiTick();
    }

    // ── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        File f = new File(plugin.getDataFolder(), "bosses.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        // Load skills
        ConfigurationSection skillsSec = cfg.getConfigurationSection("skills");
        if (skillsSec != null) {
            for (String skillId : skillsSec.getKeys(false)) {
                ConfigurationSection s = skillsSec.getConfigurationSection(skillId);
                if (s == null) continue;

                BossSkillData.SkillType type;
                try { type = BossSkillData.SkillType.valueOf(s.getString("type", "AOE_DAMAGE")); }
                catch (IllegalArgumentException e) { type = BossSkillData.SkillType.AOE_DAMAGE; }

                Particle particle;
                try { particle = Particle.valueOf(s.getString("particle", "FLAME")); }
                catch (IllegalArgumentException e) { particle = Particle.FLAME; }

                skillDataMap.put(skillId, new BossSkillData.Builder(skillId, s.getString("name", skillId), type)
                        .cooldown(s.getInt("cooldown", 10))
                        .damage(s.getDouble("damage", 5))
                        .range(s.getDouble("radius", s.getDouble("range", 5)))
                        .angle(s.getDouble("angle", 60))
                        .knockback(s.getDouble("knockback", 0))
                        .mythicMob(s.getString("mythicmob-id", ""))
                        .count(s.getInt("count", 1))
                        .potion(s.getString("potion-type", "SLOWNESS"))
                        .amplifier(s.getInt("potion-amplifier", 0))
                        .duration(s.getInt("duration", 60))
                        .meteor(s.getInt("count", 4), s.getDouble("radius", 8))
                        .particle(particle)
                        .build());
            }
        }

        // Load bosses
        ConfigurationSection bossesSec = cfg.getConfigurationSection("bosses");
        if (bossesSec != null) {
            for (String bossId : bossesSec.getKeys(false)) {
                ConfigurationSection b = bossesSec.getConfigurationSection(bossId);
                if (b == null) continue;

                List<BossData.Phase> phases = new ArrayList<>();
                List<Map<?, ?>> phaseList = b.getMapList("phases");
                for (Map<?, ?> rawMap : phaseList) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pm = (Map<String, Object>) rawMap;

                    int    threshold = pm.containsKey("threshold") ? ((Number) pm.get("threshold")).intValue() : 100;
                    String name      = pm.containsKey("name")      ? (String) pm.get("name") : "Phase";
                    double speed     = pm.containsKey("speed")     ? ((Number) pm.get("speed")).doubleValue() : 0.3;
                    String announce  = pm.containsKey("announce")  ? (String) pm.get("announce") : null;

                    @SuppressWarnings("unchecked")
                    List<String> skills = pm.containsKey("skills")
                            ? (List<String>) pm.get("skills")
                            : new ArrayList<>();

                    phases.add(new BossData.Phase(threshold, name, skills, speed, announce));
                }
                phases.sort((a, bb) -> bb.getThreshold() - a.getThreshold());

                ConfigurationSection rSec = b.getConfigurationSection("rewards");
                BossData.BossReward reward = new BossData.BossReward(
                        rSec != null ? rSec.getInt("exp", 0)   : 0,
                        rSec != null ? rSec.getInt("money", 0) : 0,
                        rSec != null ? rSec.getStringList("commands") : new ArrayList<>()
                );

                String lootConfig = b.getString("loot", "");

                bossDataMap.put(bossId, new BossData(
                        bossId,
                        b.getString("display-name", bossId),
                        b.getString("mythicmob-id", ""),
                        b.getDouble("max-hp", 1000),
                        phases,
                        reward,
                        lootConfig
                ));
            }
        }
        plugin.getLogger().info("Loaded " + bossDataMap.size() + " boss(es), " + skillDataMap.size() + " skill(s).");
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    public ActiveBoss spawnBoss(String bossId, Location loc) {
        BossData data = bossDataMap.get(bossId);
        if (data == null) { plugin.getLogger().warning("Boss khong ton tai: " + bossId); return null; }

        LivingEntity entity;
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs") && !data.getMythicMobId().isEmpty()) {
            try {
                io.lumine.mythic.api.adapters.AbstractLocation abstractLoc =
                        io.lumine.mythic.bukkit.BukkitAdapter.adapt(loc);
                var mythicMob = MythicBukkit.inst().getMobManager()
                        .spawnMob(data.getMythicMobId(), abstractLoc, 1);
                entity = (LivingEntity) mythicMob.getEntity().getBukkitEntity();
            } catch (Exception e) {
                entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
            }
        } else {
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
        }

        entity.setCustomName(color(data.getDisplayName()));
        entity.setCustomNameVisible(false); // Dung BossBar thay the
        entity.setRemoveWhenFarAway(false);

        ActiveBoss active = new ActiveBoss(data, entity);
        activeBosses.put(entity.getUniqueId(), active);

        // Tao BossBar
        BossBar bar = Bukkit.createBossBar(
                color(data.getDisplayName()),
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );
        bar.setProgress(1.0);
        bossBars.put(entity.getUniqueId(), bar);

        // Announce
        AnnounceUtil.broadcast(data.getDisplayName() + " &fxuat hien!");
        return active;
    }

    // ── Damage ────────────────────────────────────────────────────────────────

    public void handleDamage(UUID entityUuid, double damage, UUID damagerUuid) {
        ActiveBoss boss = activeBosses.get(entityUuid);
        if (boss == null) return;

        boolean dead = boss.damage(damage, damagerUuid);

        // Update BossBar
        BossBar bar = bossBars.get(entityUuid);
        if (bar != null) bar.setProgress(Math.max(0, boss.getHpPercent() / 100.0));

        // Phase change
        BossData.Phase newPhase = boss.checkPhaseChange();
        if (newPhase != null) {
            Bukkit.getPluginManager().callEvent(new BossPhaseChangeEvent(boss, newPhase));

            // Update BossBar color theo phase
            if (bar != null) {
                double pct = boss.getHpPercent();
                bar.setColor(pct > 60 ? BarColor.GREEN : pct > 30 ? BarColor.YELLOW : BarColor.RED);
            }

            // Apply speed
            try {
                boss.getEntity().getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)
                        .setBaseValue(newPhase.getSpeed());
            } catch (Exception ignored) {}

            // Announce phase change den player trong range
            List<Player> nearby = getNearbyPlayers(boss.getEntity().getLocation(), 100);
            AnnounceUtil.bossPhaseChange(nearby,
                    boss.getData().getDisplayName(), newPhase.getName(), newPhase.getAnnounce());
        }

        // Add player vao BossBar
        Player damager = Bukkit.getPlayer(damagerUuid);
        if (damager != null && bar != null && !bar.getPlayers().contains(damager)) {
            bar.addPlayer(damager);
        }

        if (dead) handleDeath(boss);
    }

    private void handleDeath(ActiveBoss boss) {
        activeBosses.remove(boss.getEntity().getUniqueId());

        // Xoa BossBar
        BossBar bar = bossBars.remove(boss.getEntity().getUniqueId());
        if (bar != null) { bar.removeAll(); bar.setVisible(false); }

        boss.getEntity().remove();

        Bukkit.getPluginManager().callEvent(new BossDeathEvent(boss));

        // Drop loot tai vi tri boss
        LootTable loot = LootTable.fromString(boss.getData().getLootConfig());
        loot.dropAt(boss.getEntity().getLocation());

        // Phan thuong
        for (UUID uuid : boss.getDamagers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            giveReward(p, boss.getData().getReward());
        }

        // Announce
        AnnounceUtil.bossDeath(boss.getData().getDisplayName());
    }

    private void giveReward(Player player, BossData.BossReward reward) {
        try { dev.elysium.core.api.CoreAPI.addExp(player, reward.getExp()); } catch (Exception ignored) {}
        dev.elysium.core.api.CoreAPI.addBalance(player, reward.getMoney());
        for (String cmd : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        // Hook Weapon EXP - Boss cho nhieu EXP nhat (x1.5)
        try {
            Class<?> weaponAPI = Class.forName("dev.elysium.weapon.api.WeaponAPI");
            String weaponId = (String) weaponAPI.getMethod("getHeldWeaponId", org.bukkit.entity.Player.class)
                    .invoke(null, player);
            if (weaponId != null) {
                long weaponExp = Math.max(100, reward.getExp() / 5);
                weaponAPI.getMethod("addWeaponExp", org.bukkit.entity.Player.class, String.class, long.class, String.class)
                        .invoke(null, player, weaponId, weaponExp, "BOSS");
            }
        } catch (ClassNotFoundException ignored) {
            // ElysiumWeapon chua duoc cai
        } catch (Exception e) {
            plugin.getLogger().warning("[Adventure] Weapon EXP hook error: " + e.getMessage());
        }

        player.sendMessage(color("&5[Boss] &aNhan thuong: &e+" + reward.getExp()
                + " EXP &f| &a+" + reward.getMoney() + " coin"));
    }

    // ── AI Tick ───────────────────────────────────────────────────────────────

    private void startAiTick() {
        aiTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (ActiveBoss boss : new ArrayList<>(activeBosses.values())) {
                    if (!boss.isAlive()) { activeBosses.remove(boss.getEntity().getUniqueId()); continue; }
                    tickBossAi(boss);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void tickBossAi(ActiveBoss boss) {
        BossData.Phase phase = boss.getCurrentPhase();
        for (String skillId : phase.getSkills()) {
            BossSkillData skill = skillDataMap.get(skillId);
            if (skill == null) continue;
            if (boss.isSkillReady(skillId, skill.getCooldown())) {
                executor.execute(boss, skill);
                boss.markSkillUsed(skillId);
                break;
            }
        }

        // Update BossBar title voi HP
        BossBar bar = bossBars.get(boss.getEntity().getUniqueId());
        if (bar != null) {
            bar.setTitle(color(boss.getData().getDisplayName()
                    + " &f| " + String.format("%.0f", boss.getCurrentHp())
                    + "/" + (int) boss.getData().getMaxHp()
                    + " &7[" + boss.getCurrentPhase().getName() + "]"));
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private List<Player> getNearbyPlayers(Location loc, double range) {
        List<Player> list = new ArrayList<>();
        for (Entity e : loc.getWorld().getNearbyEntities(loc, range, range, range)) {
            if (e instanceof Player p) list.add(p);
        }
        return list;
    }

    public void shutdown() {
        if (aiTask != null) aiTask.cancel();
        for (BossBar bar : bossBars.values()) { bar.removeAll(); bar.setVisible(false); }
        bossBars.clear();
        for (ActiveBoss boss : activeBosses.values()) boss.getEntity().remove();
        activeBosses.clear();
    }

    public ActiveBoss getActiveBoss(UUID entityUuid) { return activeBosses.get(entityUuid); }
    public boolean    isBoss(UUID entityUuid)        { return activeBosses.containsKey(entityUuid); }
    public int        getBossCount()                 { return bossDataMap.size(); }
    public Set<String> getBossIds()                  { return bossDataMap.keySet(); }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
