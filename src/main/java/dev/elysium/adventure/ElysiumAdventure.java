package dev.elysium.adventure;

import dev.elysium.adventure.api.AdventureAPI;
import dev.elysium.adventure.boss.BossManager;
import dev.elysium.adventure.command.AdventureCommand;
import dev.elysium.adventure.command.BossAdminCommand;
import dev.elysium.adventure.command.DungeonCommand;
import dev.elysium.adventure.command.PartyCommand;
import dev.elysium.adventure.config.AdventureConfig;
import dev.elysium.adventure.dungeon.DungeonManager;
import dev.elysium.adventure.gui.GuiListener;
import dev.elysium.adventure.listener.BossListener;
import dev.elysium.adventure.listener.DungeonListener;
import dev.elysium.adventure.listener.PartyListener;
import dev.elysium.adventure.party.PartyManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ElysiumAdventure extends JavaPlugin {

    private static ElysiumAdventure instance;

    private AdventureConfig adventureConfig;
    private PartyManager    partyManager;
    private BossManager     bossManager;
    private DungeonManager  dungeonManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("bosses.yml", false);
        saveResource("dungeons.yml", false);

        adventureConfig = new AdventureConfig(this);
        partyManager    = new PartyManager(this);
        bossManager     = new BossManager(this);
        dungeonManager  = new DungeonManager(this);

        AdventureAPI.init(this);

        // Commands
        getCommand("adventure").setExecutor(new AdventureCommand(this));
        getCommand("party").setExecutor(new PartyCommand(this));
        getCommand("dungeon").setExecutor(new DungeonCommand(this));
        getCommand("boss").setExecutor(new BossAdminCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PartyListener(this), this);
        getServer().getPluginManager().registerEvents(new BossListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        getLogger().info("=== ElysiumAdventure v" + getDescription().getVersion() + " enabled! ===");
        getLogger().info("Bosses: " + bossManager.getBossCount()
                + " | Dungeons: " + dungeonManager.getDungeonCount());
    }

    @Override
    public void onDisable() {
        if (dungeonManager != null) dungeonManager.shutdown();
        if (bossManager    != null) bossManager.shutdown();
        getLogger().info("ElysiumAdventure disabled.");
    }

    public static ElysiumAdventure getInstance() { return instance; }
    public AdventureConfig getAdventureConfig()  { return adventureConfig; }
    public PartyManager    getPartyManager()     { return partyManager; }
    public BossManager     getBossManager()      { return bossManager; }
    public DungeonManager  getDungeonManager()   { return dungeonManager; }
}
