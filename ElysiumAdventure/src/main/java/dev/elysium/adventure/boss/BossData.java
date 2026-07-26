package dev.elysium.adventure.boss;

import java.util.List;

public class BossData {

    private final String       id;
    private final String       displayName;
    private final String       mythicMobId;
    private final double       maxHp;
    private final List<Phase>  phases;
    private final BossReward   reward;

    public BossData(String id, String displayName, String mythicMobId,
                    double maxHp, List<Phase> phases, BossReward reward) {
        this.id          = id;
        this.displayName = displayName;
        this.mythicMobId = mythicMobId;
        this.maxHp       = maxHp;
        this.phases      = phases;
        this.reward      = reward;
    }

    /** Tra ve Phase tuong ung voi phan tram HP hien tai */
    public Phase getPhaseForHp(double currentHp) {
        double pct = (currentHp / maxHp) * 100.0;
        Phase result = phases.get(0);
        for (Phase p : phases) {
            if (pct <= p.getThreshold()) result = p;
        }
        return result;
    }

    public String      getId()          { return id; }
    public String      getDisplayName() { return displayName; }
    public String      getMythicMobId() { return mythicMobId; }
    public double      getMaxHp()       { return maxHp; }
    public List<Phase> getPhases()      { return phases; }
    public BossReward  getReward()      { return reward; }

    // ── Inner: Phase ──────────────────────────────────────────────────────────

    public static class Phase {
        private final int          threshold;   // % HP
        private final String       name;
        private final List<String> skills;
        private final double       speed;
        private final String       announce;    // null = khong thong bao

        public Phase(int threshold, String name, List<String> skills, double speed, String announce) {
            this.threshold = threshold;
            this.name      = name;
            this.skills    = skills;
            this.speed     = speed;
            this.announce  = announce;
        }

        public int          getThreshold() { return threshold; }
        public String       getName()      { return name; }
        public List<String> getSkills()    { return skills; }
        public double       getSpeed()     { return speed; }
        public String       getAnnounce()  { return announce; }
    }

    // ── Inner: Reward ─────────────────────────────────────────────────────────

    public static class BossReward {
        private final int          exp;
        private final int          money;
        private final List<String> commands;

        public BossReward(int exp, int money, List<String> commands) {
            this.exp      = exp;
            this.money    = money;
            this.commands = commands;
        }

        public int          getExp()      { return exp; }
        public int          getMoney()    { return money; }
        public List<String> getCommands() { return commands; }
    }
}
