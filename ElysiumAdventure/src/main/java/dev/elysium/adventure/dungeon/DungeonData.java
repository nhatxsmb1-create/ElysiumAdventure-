package dev.elysium.adventure.dungeon;

import org.bukkit.Location;

public class DungeonData {

    private final String id;
    private final String displayName;
    private final String description;
    private final int    minPlayers;
    private final int    maxPlayers;
    private final int    minLevel;
    private final int    timeLimitSeconds;
    private final int    cooldownSeconds;
    private final String bossId;
    private final String worldTemplate;
    private final double spawnX, spawnY, spawnZ;
    private final float  spawnYaw;
    private final double bossX, bossY, bossZ;

    public DungeonData(String id, String displayName, String description,
                       int minPlayers, int maxPlayers, int minLevel,
                       int timeLimitSeconds, int cooldownSeconds,
                       String bossId, String worldTemplate,
                       double spawnX, double spawnY, double spawnZ, float spawnYaw,
                       double bossX, double bossY, double bossZ) {
        this.id              = id;
        this.displayName     = displayName;
        this.description     = description;
        this.minPlayers      = minPlayers;
        this.maxPlayers      = maxPlayers;
        this.minLevel        = minLevel;
        this.timeLimitSeconds= timeLimitSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.bossId          = bossId;
        this.worldTemplate   = worldTemplate;
        this.spawnX = spawnX; this.spawnY = spawnY; this.spawnZ = spawnZ;
        this.spawnYaw        = spawnYaw;
        this.bossX  = bossX;  this.bossY  = bossY;  this.bossZ  = bossZ;
    }

    public String getId()              { return id; }
    public String getDisplayName()     { return displayName; }
    public String getDescription()     { return description; }
    public int    getMinPlayers()      { return minPlayers; }
    public int    getMaxPlayers()      { return maxPlayers; }
    public int    getMinLevel()        { return minLevel; }
    public int    getTimeLimitSeconds(){ return timeLimitSeconds; }
    public int    getCooldownSeconds() { return cooldownSeconds; }
    public String getBossId()          { return bossId; }
    public String getWorldTemplate()   { return worldTemplate; }
    public double getSpawnX()          { return spawnX; }
    public double getSpawnY()          { return spawnY; }
    public double getSpawnZ()          { return spawnZ; }
    public float  getSpawnYaw()        { return spawnYaw; }
    public double getBossX()           { return bossX; }
    public double getBossY()           { return bossY; }
    public double getBossZ()           { return bossZ; }
}
