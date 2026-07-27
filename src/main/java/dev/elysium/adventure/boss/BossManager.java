package dev.elysium.adventure.boss;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.event.BossDeathEvent;
import dev.elysium.adventure.event.BossPhaseChangeEvent;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class BossManager {

    private final ElysiumAdventure plugin;
    private final BossSkillExecutor executor;

    // Dinh nghia boss tu config
    private final Map<String, BossData>     bossDataMap  = new HashMap<>();
    private final Map<String, BossSkillData> skillDataMap = new HashMap<>();

    // Boss dang song: entity UUID -> ActiveBoss
    private final Map<UUID, ActiveBoss> activeBosses = new HashMap<>();

    private BukkitTask aiTask;

    public BossManager(ElysiumAdventure plugin) {
        this.plugin   = plugin;
        this.executor = new BossSkillExecutor(plugin);
        loadConfig();
        startAiTick();
    }

    // ── Config Loading ────────────────────────────────────────────────────────

    private void loadConfig() {
        File f = new File(plugin.getDataFolder(), "bosses.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        // Load skills truoc
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

                BossSkillData skill = new BossSkillData.Builder(skillId, s.getString("name", skillId), type)
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
                        .build();

                skillDataMap.put(skillId, skill);
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
                for (Map<?, ?> pm : phaseList) {
                    int           threshold = (int) pm.getOrDefault("threshold", 100);
                    String        name      = (String) pm.getOrDefault("name", "Phase");
                    @SuppressWarnings("unchecked")
                    List<String>  skills    = (List<String>) pm.getOrDefault("skills", new java.util.ArrayList<String>());
                    double        speed     = pm.containsKey("speed") ?
                            ((Number) pm.get("speed")).doubleValue() : 0.3;
                    String        announce  = (String) pm.get("announce");
                    phases.add(new BossData.Phase(threshold, name, skills, speed, announce));
                }
                // Sort giam dan theo threshold
                phases.sort((a, bb) -> bb.getThreshold() - a.getThreshold());

                ConfigurationSection rSec = b.getConfigurationSection("rewards");
                BossData.BossReward reward = new BossData.BossReward(
                        rSec != null ? rSec.getInt("exp", 0)   : 0,
                        rSec != null ? rSec.getInt("money", 0) : 0,
                        rSec != null ? rSec.getStringList("commands") : new java.util.ArrayList<>()
                );

                bossDataMap.put(bossId, new BossData(
                        bossId,
                        b.getString("display-name", bossId),
                        b.getString("mythicmob-id", ""),
                        b.getDouble("max-hp", 1000),
                        phases,
                        reward
                ));
            }
        }

        plugin.getLogger().info("Loaded " + bossDataMap.size() + " boss(es), "
                + skillDataMap.size() + " skill(s).");
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    public ActiveBoss spawnBoss(String bossId, Location loc) {
        BossData data = bossDataMap.get(bossId);
        if (data == null) {
            plugin.getLogger().warning("Boss khong ton tai: " + bossId);
            return null;
        }

        LivingEntity entity;
        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")
                && !data.getMythicMobId().isEmpty()) {
            // Dung MythicMobs chi de spawn entity
            try {
                var mythicMob = MythicBukkit.inst().getMobManager()
                        .spawnMob(data.getMythicMobId(), loc, 1);
                entity = (LivingEntity) mythicMob.getEntity().getBukkitEntity();
            } catch (Exception e) {
                plugin.getLogger().warning("Khong spawn duoc MythicMob: " + data.getMythicMobId()
                        + " — spawn Wither Skeleton thay the.");
                entity = (LivingEntity) loc.getWorld().spawnEntity(loc,
                        org.bukkit.entity.EntityType.WITHER_SKELETON);
            }
        } else {
            // Fallback: spawn Wither Skeleton
            entity = (LivingEntity) loc.getWorld().spawnEntity(loc,
                    org.bukkit.entity.EntityType.WITHER_SKELETON);
        }

        entity.setCustomName(color(data.getDisplayName()));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);

        ActiveBoss active = new ActiveBoss(data, entity);
        activeBosses.put(entity.getUniqueId(), active);

        // Thong bao toan server
        Bukkit.broadcastMessage(color("&5&l[Boss] &r" + data.getDisplayName()
                + " &fxuat hien tai &e" + formatLoc(loc) + "!"));
        return active;
    }

    // ── Damage Handling ───────────────────────────────────────────────────────

    /** Goi tu BossListener khi entity bi tan cong */
    public void handleDamage(UUID entityUuid, double damage, UUID damagerUuid) {
        ActiveBoss boss = activeBosses.get(entityUuid);
        if (boss == null) return;

        boolean dead = boss.damage(damage, damagerUuid);

        // Kiem tra phase change
        BossData.Phase newPhase = boss.checkPhaseChange();
        if (newPhase != null) {
            BossPhaseChangeEvent event = new BossPhaseChangeEvent(boss, newPhase);
            Bukkit.getPluginManager().callEvent(event);
            if (newPhase.getAnnounce() != null) {
                Bukkit.broadcastMessage(color(newPhase.getAnnounce()));
            }
            // Ap dung speed moi
            try {
                boss.getEntity().getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)
                        .setBaseValue(newPhase.getSpeed());
            } catch (Exception ignored) {}
        }

        if (dead) handleDeath(boss);
    }

    private void handleDeath(ActiveBoss boss) {
        activeBosses.remove(boss.getEntity().getUniqueId());
        boss.getEntity().remove();

        // Fire custom event
        BossDeathEvent event = new BossDeathEvent(boss);
        Bukkit.getPluginManager().callEvent(event);

        // Phat thuong cho tat ca player da gay damage
        BossData.BossReward reward = boss.getData().getReward();
        for (UUID uuid : boss.getDamagers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            giveReward(p, reward);
        }

        Bukkit.broadcastMessage(color("&5&l[Boss] &r" + boss.getData().getDisplayName()
                + " &fda bi tieu diet!"));
    }

    private void giveReward(Player player, BossData.BossReward reward) {
        // Exp qua CoreAPI
        try {
            dev.elysium.core.api.CoreAPI.addExp(player, reward.getExp());
        } catch (Exception ignored) {}

        // Money qua CoreAPI (Vault first, fallback internal)
        dev.elysium.core.api.CoreAPI.addBalance(player, reward.getMoney());

        // Commands
        for (String cmd : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
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
                    if (!boss.isAlive()) {
                        activeBosses.remove(boss.getEntity().getUniqueId());
                        continue;
                    }
                    tickBossAi(boss);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // moi 1 giay
    }

    private void tickBossAi(ActiveBoss boss) {
        BossData.Phase phase = boss.getCurrentPhase();

        // Thu tu cac skill trong phase, dung cai dau tien het cooldown
        for (String skillId : phase.getSkills()) {
            BossSkillData skill = skillDataMap.get(skillId);
            if (skill == null) continue;
            if (boss.isSkillReady(skillId, skill.getCooldown())) {
                executor.execute(boss, skill);
                boss.markSkillUsed(skillId);
                break; // Chi dung 1 skill moi tick
            }
        }

        // Update HP display tren custom name
        double pct = boss.getHpPercent();
        String bar = buildHpBar(pct);
        boss.getEntity().setCustomName(color(boss.getData().getDisplayName()
                + "\n" + bar + " &f" + String.format("%.0f", boss.getCurrentHp())
                + "/" + (int) boss.getData().getMaxHp()));
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private String buildHpBar(double pct) {
        int filled = (int) (pct / 10);
        StringBuilder bar = new StringBuilder("&c[");
        for (int i = 0; i < 10; i++) bar.append(i < filled ? "❤" : "&8❤&c");
        bar.append("]");
        return bar.toString();
    }

    public void shutdown() {
        if (aiTask != null) aiTask.cancel();
        for (ActiveBoss boss : activeBosses.values()) boss.getEntity().remove();
        activeBosses.clear();
    }

    public ActiveBoss getActiveBoss(UUID entityUuid) { return activeBosses.get(entityUuid); }
    public boolean    isBoss(UUID entityUuid)        { return activeBosses.containsKey(entityUuid); }
    public int        getBossCount()                 { return bossDataMap.size(); }
    public Set<String> getBossIds()                  { return bossDataMap.keySet(); }

    private String color(String s) { return s.replace("&", "\u00a7"); }
    private String formatLoc(Location l) {
        return String.format("%.0f, %.0f, %.0f", l.getX(), l.getY(), l.getZ());
    }
        }
