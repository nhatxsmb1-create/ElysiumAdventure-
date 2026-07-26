package dev.elysium.adventure.config;

import dev.elysium.adventure.ElysiumAdventure;
import org.bukkit.configuration.file.FileConfiguration;

public class AdventureConfig {

    private final ElysiumAdventure plugin;
    private final FileConfiguration cfg;

    public AdventureConfig(ElysiumAdventure plugin) {
        this.plugin = plugin;
        this.cfg    = plugin.getConfig();
    }

    public String getPrefix()         { return cfg.getString("prefix", "&5[Adventure] &r"); }
    public int    getPartyMaxSize()   { return cfg.getInt("party.max-size", 6); }
    public int    getPartyInviteTimeout() { return cfg.getInt("party.invite-timeout", 30); }
    public int    getDungeonCooldown()    { return cfg.getInt("dungeon.cooldown", 3600); }
    public int    getDungeonTimeout()     { return cfg.getInt("dungeon.timeout", 60); }
    public boolean isBossHpBarGlobal()   { return cfg.getBoolean("boss.hp-bar-global", false); }
    public int    getBossHpBarRange()     { return cfg.getInt("boss.hp-bar-range", 50); }
}
