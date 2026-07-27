package dev.elysium.adventure.boss;

import dev.elysium.adventure.ElysiumAdventure;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BossSkillExecutor {

    private final ElysiumAdventure plugin;

    public BossSkillExecutor(ElysiumAdventure plugin) {
        this.plugin = plugin;
    }

    public void execute(ActiveBoss boss, BossSkillData skill) {
        LivingEntity entity = boss.getEntity();
        if (!entity.isValid()) return;

        switch (skill.getType()) {
            case CONE_DAMAGE   -> executeCone(entity, skill);
            case AOE_DAMAGE    -> executeAoe(entity, skill);
            case PROJECTILE    -> executeProjectile(entity, skill);
            case SUMMON        -> executeSummon(entity, skill);
            case DEBUFF        -> executeDebuff(entity, skill);
            case METEOR        -> executeMeteor(entity, skill);
        }
    }

    private void executeCone(LivingEntity boss, BossSkillData skill) {
        Location loc    = boss.getLocation();
        Vector   facing = loc.getDirection().normalize();

        for (Entity nearby : boss.getNearbyEntities(skill.getRange(), 4, skill.getRange())) {
            if (!(nearby instanceof Player target)) continue;
            Vector toTarget = target.getLocation().subtract(loc).toVector().normalize();
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, facing.dot(toTarget)))));
            if (angle <= skill.getAngle() / 2.0) {
                target.damage(skill.getDamage(), boss);
                spawnParticles(loc.getWorld(), skill.getParticle(), target.getLocation().add(0,1,0), 10, 0.3);
            }
        }
        spawnParticles(loc.getWorld(), skill.getParticle(), loc.clone().add(0,1,0), 30, 1.5);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);
    }

    private void executeAoe(LivingEntity boss, BossSkillData skill) {
        Location loc = boss.getLocation();
        for (Entity nearby : boss.getNearbyEntities(skill.getRange(), 4, skill.getRange())) {
            if (!(nearby instanceof Player target)) continue;
            target.damage(skill.getDamage(), boss);
            if (skill.getKnockback() > 0) {
                Vector kb = target.getLocation().subtract(loc).toVector()
                        .normalize().multiply(skill.getKnockback());
                kb.setY(0.4);
                target.setVelocity(kb);
            }
        }
        spawnParticles(loc.getWorld(), skill.getParticle(), loc.clone().add(0,0.5,0), 50, skill.getRange() * 0.5);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }

    private void executeProjectile(LivingEntity boss, BossSkillData skill) {
        Player target = getNearestPlayer(boss, 30);
        if (target == null) return;

        Location from = boss.getEyeLocation();
        Vector   dir  = target.getEyeLocation().subtract(from).toVector().normalize();

        Fireball fb = boss.getWorld().spawn(from, Fireball.class);
        fb.setDirection(dir.multiply(1.5));
        fb.setShooter(boss);
        fb.setIsIncendiary(false);
        fb.setYield(0f);

        fb.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "boss_projectile_damage"),
                org.bukkit.persistence.PersistentDataType.DOUBLE,
                skill.getDamage()
        );
        spawnParticles(from.getWorld(), skill.getParticle(), from, 5, 0.2);
        boss.getWorld().playSound(from, Sound.ENTITY_BLAZE_SHOOT, 1f, 1.2f);
    }

    private void executeSummon(LivingEntity boss, BossSkillData skill) {
        Location loc = boss.getLocation();
        if (!Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            plugin.getLogger().warning("MythicMobs chua bat, bo qua skill SUMMON: " + skill.getId());
            return;
        }
        try {
            // Dung BukkitAdapter de chuyen Location sang AbstractLocation
            io.lumine.mythic.bukkit.BukkitAdapter adapter = io.lumine.mythic.bukkit.BukkitAdapter.INSTANCE;
            io.lumine.mythic.api.adapters.AbstractLocation abstractLoc = adapter.adapt(loc);

            io.lumine.mythic.bukkit.MythicBukkit.inst()
                    .getMobManager()
                    .spawnMob(skill.getMythicMobId(), abstractLoc, skill.getCount());
        } catch (Exception e) {
            plugin.getLogger().warning("Khong the spawn mythic mob: " + skill.getMythicMobId() + " - " + e.getMessage());
        }
        spawnParticles(loc.getWorld(), Particle.PORTAL, loc.clone().add(0,1,0), 40, 1.0);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
    }

    private void executeDebuff(LivingEntity boss, BossSkillData skill) {
        Location loc  = boss.getLocation();
        PotionEffectType type = PotionEffectType.getByName(skill.getPotionType());
        if (type == null) return;

        for (Entity nearby : boss.getNearbyEntities(skill.getRange(), 4, skill.getRange())) {
            if (!(nearby instanceof Player target)) continue;
            target.addPotionEffect(new PotionEffect(type, skill.getDuration(), skill.getAmplifier()));
        }
        spawnParticles(loc.getWorld(), skill.getParticle(), loc.clone().add(0,1,0), 30, skill.getRange() * 0.4);
        loc.getWorld().playSound(loc, Sound.ENTITY_WITCH_THROW, 1f, 0.8f);
    }

    private void executeMeteor(LivingEntity boss, BossSkillData skill) {
        Location center = boss.getLocation();
        World    world  = center.getWorld();
        int      count  = skill.getMeteorCount();
        double   radius = skill.getMeteorRadius();

        for (int i = 0; i < count; i++) {
            final int delay = i * 10;
            new BukkitRunnable() {
                @Override public void run() {
                    if (!boss.isValid()) return;
                    double angle = Math.random() * Math.PI * 2;
                    double dist  = Math.random() * radius;
                    double x     = center.getX() + Math.cos(angle) * dist;
                    double z     = center.getZ() + Math.sin(angle) * dist;

                    Location impact = new Location(world, x, center.getY(), z);
                    world.spawnParticle(Particle.LAVA, impact.clone().add(0,0.1,0), 15, 0.5, 0, 0.5, 0);

                    new BukkitRunnable() {
                        @Override public void run() {
                            world.createExplosion(impact, 0f, false, false);
                            world.spawnParticle(Particle.EXPLOSION, impact, 5, 0.5, 0.5, 0.5, 0);
                            world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                            for (Entity e : impact.getWorld().getNearbyEntities(impact, 3, 3, 3)) {
                                if (e instanceof Player p) p.damage(skill.getDamage(), boss);
                            }
                        }
                    }.runTaskLater(plugin, 30L);
                }
            }.runTaskLater(plugin, delay);
        }
    }

    private Player getNearestPlayer(LivingEntity boss, double range) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : boss.getNearbyEntities(range, range, range)) {
            if (!(e instanceof Player p)) continue;
            double d = boss.getLocation().distanceSquared(p.getLocation());
            if (d < minDist) { minDist = d; nearest = p; }
        }
        return nearest;
    }

    private void spawnParticles(World world, Particle type, Location loc, int count, double spread) {
        if (world == null) return;
        world.spawnParticle(type, loc, count, spread, spread, spread, 0);
    }
            }
