package dev.elysium.adventure.boss;

import org.bukkit.Particle;

public class BossSkillData {

    public enum SkillType {
        CONE_DAMAGE, AOE_DAMAGE, PROJECTILE, SUMMON, DEBUFF, METEOR
    }

    private final String    id;
    private final String    name;
    private final SkillType type;
    private final int       cooldown;

    // Damage skills
    private final double  damage;
    private final double  range;
    private final double  angle;
    private final double  knockback;

    // Summon
    private final String  mythicMobId;
    private final int     count;

    // Debuff
    private final String  potionType;
    private final int     amplifier;
    private final int     duration;

    // Meteor
    private final int     meteorCount;
    private final double  meteorRadius;

    // Visual
    private final Particle particle;

    private BossSkillData(Builder b) {
        this.id          = b.id;
        this.name        = b.name;
        this.type        = b.type;
        this.cooldown    = b.cooldown;
        this.damage      = b.damage;
        this.range       = b.range;
        this.angle       = b.angle;
        this.knockback   = b.knockback;
        this.mythicMobId = b.mythicMobId;
        this.count       = b.count;
        this.potionType  = b.potionType;
        this.amplifier   = b.amplifier;
        this.duration    = b.duration;
        this.meteorCount = b.meteorCount;
        this.meteorRadius= b.meteorRadius;
        this.particle    = b.particle;
    }

    public String     getId()          { return id; }
    public String     getName()        { return name; }
    public SkillType  getType()        { return type; }
    public int        getCooldown()    { return cooldown; }
    public double     getDamage()      { return damage; }
    public double     getRange()       { return range; }
    public double     getAngle()       { return angle; }
    public double     getKnockback()   { return knockback; }
    public String     getMythicMobId() { return mythicMobId; }
    public int        getCount()       { return count; }
    public String     getPotionType()  { return potionType; }
    public int        getAmplifier()   { return amplifier; }
    public int        getDuration()    { return duration; }
    public int        getMeteorCount() { return meteorCount; }
    public double     getMeteorRadius(){ return meteorRadius; }
    public Particle   getParticle()    { return particle; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder {
        String id, name, mythicMobId, potionType;
        SkillType type;
        int cooldown, count, amplifier, duration, meteorCount;
        double damage, range, angle, knockback, meteorRadius;
        Particle particle = Particle.FLAME;

        public Builder(String id, String name, SkillType type) {
            this.id = id; this.name = name; this.type = type;
        }
        public Builder cooldown(int v)      { cooldown = v;     return this; }
        public Builder damage(double v)     { damage = v;       return this; }
        public Builder range(double v)      { range = v;        return this; }
        public Builder angle(double v)      { angle = v;        return this; }
        public Builder knockback(double v)  { knockback = v;    return this; }
        public Builder mythicMob(String v)  { mythicMobId = v;  return this; }
        public Builder count(int v)         { count = v;        return this; }
        public Builder potion(String v)     { potionType = v;   return this; }
        public Builder amplifier(int v)     { amplifier = v;    return this; }
        public Builder duration(int v)      { duration = v;     return this; }
        public Builder meteor(int cnt, double r) { meteorCount = cnt; meteorRadius = r; return this; }
        public Builder particle(Particle v) { particle = v;     return this; }
        public BossSkillData build()        { return new BossSkillData(this); }
    }
}
