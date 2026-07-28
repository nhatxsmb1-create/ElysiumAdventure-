package dev.elysium.adventure.wave;

import org.bukkit.Location;

import java.util.List;
import java.util.Map;

public class WaveDungeonData {

    public enum Difficulty { NORMAL, HARD, NIGHTMARE }

    private final String id;
    private final String displayName;
    private final String description;
    private final String worldTemplate;
    private final int    minPlayers;
    private final int    maxPlayers;
    private final double spawnX, spawnY, spawnZ;
    private final float  spawnYaw;
    private final List<MobSpawnPoint> mobSpawnPoints;

    // difficulty -> DifficultyConfig
    private final Map<Difficulty, DifficultyConfig> difficulties;

    public WaveDungeonData(String id, String displayName, String description,
                           String worldTemplate, int minPlayers, int maxPlayers,
                           double spawnX, double spawnY, double spawnZ, float spawnYaw,
                           List<MobSpawnPoint> mobSpawnPoints,
                           Map<Difficulty, DifficultyConfig> difficulties) {
        this.id             = id;
        this.displayName    = displayName;
        this.description    = description;
        this.worldTemplate  = worldTemplate;
        this.minPlayers     = minPlayers;
        this.maxPlayers     = maxPlayers;
        this.spawnX         = spawnX;
        this.spawnY         = spawnY;
        this.spawnZ         = spawnZ;
        this.spawnYaw       = spawnYaw;
        this.mobSpawnPoints = mobSpawnPoints;
        this.difficulties   = difficulties;
    }

    public String              getId()             { return id; }
    public String              getDisplayName()    { return displayName; }
    public String              getDescription()    { return description; }
    public String              getWorldTemplate()  { return worldTemplate; }
    public int                 getMinPlayers()     { return minPlayers; }
    public int                 getMaxPlayers()     { return maxPlayers; }
    public double              getSpawnX()         { return spawnX; }
    public double              getSpawnY()         { return spawnY; }
    public double              getSpawnZ()         { return spawnZ; }
    public float               getSpawnYaw()       { return spawnYaw; }
    public List<MobSpawnPoint> getMobSpawnPoints() { return mobSpawnPoints; }
    public DifficultyConfig    getDifficulty(Difficulty d) { return difficulties.get(d); }
    public boolean             hasDifficulty(Difficulty d) { return difficulties.containsKey(d); }

    // ── Inner: MobSpawnPoint ─────────────────────────────────────────────────

    public static class MobSpawnPoint {
        public final double x, y, z;
        public MobSpawnPoint(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    // ── Inner: DifficultyConfig ───────────────────────────────────────────────

    public static class DifficultyConfig {
        private final int                        waveCount;
        private final int                        waveDelay;     // giay
        private final WaveReward                 reward;
        // wave number (1-based) -> list of mob groups
        private final Map<Integer, List<WaveMob>> waves;

        public DifficultyConfig(int waveCount, int waveDelay,
                                WaveReward reward,
                                Map<Integer, List<WaveMob>> waves) {
            this.waveCount = waveCount;
            this.waveDelay = waveDelay;
            this.reward    = reward;
            this.waves     = waves;
        }

        public int                  getWaveCount()      { return waveCount; }
        public int                  getWaveDelay()      { return waveDelay; }
        public WaveReward           getReward()         { return reward; }
        public List<WaveMob>        getWave(int number) { return waves.getOrDefault(number, List.of()); }
    }

    // ── Inner: WaveMob ────────────────────────────────────────────────────────

    public static class WaveMob {
        private final String mythicMobId;
        private final int    level;
        private final int    count;

        public WaveMob(String mythicMobId, int level, int count) {
            this.mythicMobId = mythicMobId;
            this.level       = level;
            this.count       = count;
        }

        public String getMythicMobId() { return mythicMobId; }
        public int    getLevel()       { return level; }
        public int    getCount()       { return count; }
    }

    // ── Inner: WaveReward ─────────────────────────────────────────────────────

    public static class WaveReward {
        private final int          exp;
        private final int          money;
        private final java.util.List<String> commands;

        public WaveReward(int exp, int money, java.util.List<String> commands) {
            this.exp      = exp;
            this.money    = money;
            this.commands = commands;
        }

        public int          getExp()      { return exp; }
        public int          getMoney()    { return money; }
        public java.util.List<String> getCommands() { return commands; }
    }
}
