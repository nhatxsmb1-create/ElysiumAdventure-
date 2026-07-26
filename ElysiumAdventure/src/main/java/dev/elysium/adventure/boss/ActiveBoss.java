package dev.elysium.adventure.boss;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.*;

public class ActiveBoss {

    private final UUID           id;
    private final BossData       data;
    private final LivingEntity   entity;
    private BossData.Phase       currentPhase;
    private double               currentHp;

    // Cooldown: skillId -> last use millis
    private final Map<String, Long> skillCooldowns = new HashMap<>();

    // Danh sach player da gay damage (de chia reward)
    private final Set<UUID> damagers = new LinkedHashSet<>();

    public ActiveBoss(BossData data, LivingEntity entity) {
        this.id           = UUID.randomUUID();
        this.data         = data;
        this.entity       = entity;
        this.currentHp    = data.getMaxHp();
        this.currentPhase = data.getPhaseForHp(currentHp);

        // Set max health cho entity
        entity.setMaxHealth(data.getMaxHp());
        entity.setHealth(data.getMaxHp());
    }

    // ── Damage ────────────────────────────────────────────────────────────────

    /** Tra ve true neu boss chet */
    public boolean damage(double amount, UUID damagerUuid) {
        currentHp = Math.max(0, currentHp - amount);
        damagers.add(damagerUuid);
        return currentHp <= 0;
    }

    // ── Phase ─────────────────────────────────────────────────────────────────

    /** Tra ve phase moi neu co thay doi, null neu khong doi */
    public BossData.Phase checkPhaseChange() {
        BossData.Phase newPhase = data.getPhaseForHp(currentHp);
        if (!newPhase.getName().equals(currentPhase.getName())) {
            currentPhase = newPhase;
            return newPhase;
        }
        return null;
    }

    // ── Skill Cooldown ────────────────────────────────────────────────────────

    public boolean isSkillReady(String skillId, int cooldownSeconds) {
        Long last = skillCooldowns.get(skillId);
        if (last == null) return true;
        return System.currentTimeMillis() - last >= cooldownSeconds * 1000L;
    }

    public void markSkillUsed(String skillId) {
        skillCooldowns.put(skillId, System.currentTimeMillis());
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID           getId()           { return id; }
    public BossData       getData()         { return data; }
    public LivingEntity   getEntity()       { return entity; }
    public BossData.Phase getCurrentPhase() { return currentPhase; }
    public double         getCurrentHp()    { return currentHp; }
    public double         getHpPercent()    { return (currentHp / data.getMaxHp()) * 100.0; }
    public Set<UUID>      getDamagers()     { return Collections.unmodifiableSet(damagers); }
    public boolean        isAlive()         { return currentHp > 0 && entity.isValid(); }
}
