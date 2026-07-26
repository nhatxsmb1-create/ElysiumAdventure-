package dev.elysium.adventure.dungeon;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.boss.ActiveBoss;
import dev.elysium.adventure.party.Party;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class DungeonManager {

    private final ElysiumAdventure plugin;

    private final Map<String, DungeonData> dungeonDataMap = new HashMap<>();

    // Player UUID -> ActiveDungeon
    private final Map<UUID, ActiveDungeon> playerDungeonMap = new HashMap<>();
    // Dungeon instance ID -> ActiveDungeon
    private final Map<UUID, ActiveDungeon> activeDungeons   = new HashMap<>();
    // Player UUID -> cooldown expire millis
    private final Map<UUID, Long>          cooldowns        = new HashMap<>();

    private BukkitTask tickTask;

    public DungeonManager(ElysiumAdventure plugin) {
        this.plugin = plugin;
        loadConfig();
        startTick();
    }

    // ── Config ────────────────────────────────────────────────────────────────

    private void loadConfig() {
        File f = new File(plugin.getDataFolder(), "dungeons.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection sec = cfg.getConfigurationSection("dungeons");
        if (sec == null) return;

        for (String id : sec.getKeys(false)) {
            ConfigurationSection d = sec.getConfigurationSection(id);
            if (d == null) continue;

            ConfigurationSection spawn = d.getConfigurationSection("spawn");
            ConfigurationSection boss  = d.getConfigurationSection("boss-location");

            dungeonDataMap.put(id, new DungeonData(
                    id,
                    d.getString("display-name", id),
                    d.getString("description", ""),
                    d.getInt("min-players", 1),
                    d.getInt("max-players", 6),
                    d.getInt("min-level", 1),
                    d.getInt("time-limit", 1800),
                    d.getInt("cooldown", 3600),
                    d.getString("boss", ""),
                    d.getString("world-template", ""),
                    spawn != null ? spawn.getDouble("x") : 0,
                    spawn != null ? spawn.getDouble("y") : 64,
                    spawn != null ? spawn.getDouble("z") : 0,
                    spawn != null ? (float) spawn.getDouble("yaw") : 0,
                    boss  != null ? boss.getDouble("x")  : 0,
                    boss  != null ? boss.getDouble("y")  : 64,
                    boss  != null ? boss.getDouble("z")  : 0
            ));
        }
        plugin.getLogger().info("Loaded " + dungeonDataMap.size() + " dungeon(s).");
    }

    // ── Enter ─────────────────────────────────────────────────────────────────

    public void enterDungeon(Player player, String dungeonId) {
        DungeonData data = dungeonDataMap.get(dungeonId);
        if (data == null) { player.sendMessage(color("&cDungeon khong ton tai!")); return; }

        if (playerDungeonMap.containsKey(player.getUniqueId())) {
            player.sendMessage(color("&cBan dang trong dungeon roi!")); return;
        }

        // Kiem tra cooldown
        Long cooldownExpire = cooldowns.get(player.getUniqueId());
        if (cooldownExpire != null && System.currentTimeMillis() < cooldownExpire) {
            long remaining = (cooldownExpire - System.currentTimeMillis()) / 1000;
            player.sendMessage(color("&cBan phai cho &e" + remaining + "s &ctruoc khi vao lai!")); return;
        }

        // Kiem tra level
        try {
            int level = dev.elysium.core.api.CoreAPI.getPlayer(player).getLevel();
            if (level < data.getMinLevel()) {
                player.sendMessage(color("&cBan can level &e" + data.getMinLevel()
                        + " &cde vao dungeon nay! (Level hien tai: " + level + ")")); return;
            }
        } catch (Exception ignored) {}

        // Lay party cua player
        Party party = plugin.getPartyManager().getParty(player);
        List<UUID> members = party != null
                ? new ArrayList<>(party.getMembers())
                : List.of(player.getUniqueId());

        if (members.size() < data.getMinPlayers()) {
            player.sendMessage(color("&cDungeon nay can it nhat &e" + data.getMinPlayers()
                    + " &cnguoi!")); return;
        }
        if (members.size() > data.getMaxPlayers()) {
            player.sendMessage(color("&cDungeon nay toi da &e" + data.getMaxPlayers()
                    + " &cnguoi!")); return;
        }

        // Tao dungeon instance
        World world = createInstanceWorld(dungeonId + "_" + System.currentTimeMillis());
        ActiveDungeon dungeon = new ActiveDungeon(data, world);
        activeDungeons.put(dungeon.getId(), dungeon);

        // Teleport toan party
        Location spawnLoc = new Location(world, data.getSpawnX(), data.getSpawnY(),
                data.getSpawnZ(), data.getSpawnYaw(), 0);

        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            dungeon.addPlayer(uuid);
            playerDungeonMap.put(uuid, dungeon);
            p.teleport(spawnLoc);
            p.sendMessage(color("&5[Dungeon] &aBan da vao &f" + data.getDisplayName()
                    + " &a| Thoi gian: &e" + (data.getTimeLimitSeconds() / 60) + " phut"));
        }

        dungeon.setState(ActiveDungeon.State.IN_PROGRESS);

        // Spawn boss
        Location bossLoc = new Location(world, data.getBossX(), data.getBossY(), data.getBossZ());
        ActiveBoss boss = plugin.getBossManager().spawnBoss(data.getBossId(), bossLoc);
        dungeon.setBoss(boss);
    }

    // ── Leave ─────────────────────────────────────────────────────────────────

    public void leaveDungeon(Player player) {
        ActiveDungeon dungeon = playerDungeonMap.get(player.getUniqueId());
        if (dungeon == null) { player.sendMessage(color("&cBan khong trong dungeon nao!")); return; }

        removeFromDungeon(player.getUniqueId(), dungeon);
        teleportToSpawn(player);
        player.sendMessage(color("&cBan da roi dungeon."));
    }

    public void onBossDeath(ActiveBoss boss) {
        // Tim dungeon chua boss nay
        for (ActiveDungeon dungeon : activeDungeons.values()) {
            if (dungeon.getBoss() != null
                    && dungeon.getBoss().getId().equals(boss.getId())) {
                completeDungeon(dungeon);
                return;
            }
        }
    }

    private void completeDungeon(ActiveDungeon dungeon) {
        dungeon.complete();
        for (UUID uuid : dungeon.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(color("&5[Dungeon] &a&lHOAN THANH! " + dungeon.getData().getDisplayName()));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
            // Dat cooldown
            cooldowns.put(uuid, System.currentTimeMillis()
                    + dungeon.getData().getCooldownSeconds() * 1000L);
            playerDungeonMap.remove(uuid);
        }
        // Xoa world sau 10 giay
        destroyInstanceWorld(dungeon);
        activeDungeons.remove(dungeon.getId());
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private void startTick() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (ActiveDungeon dungeon : new ArrayList<>(activeDungeons.values())) {
                    if (dungeon.isTimedOut()) {
                        failDungeon(dungeon, "&c[Dungeon] Het gio! Dungeon that bai.");
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L); // Kiem tra moi 10 giay
    }

    private void failDungeon(ActiveDungeon dungeon, String msg) {
        dungeon.fail();
        for (UUID uuid : dungeon.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) { p.sendMessage(color(msg)); teleportToSpawn(p); }
            playerDungeonMap.remove(uuid);
        }
        destroyInstanceWorld(dungeon);
        activeDungeons.remove(dungeon.getId());
    }

    // ── World Handling ────────────────────────────────────────────────────────

    private World createInstanceWorld(String name) {
        WorldCreator wc = new WorldCreator(name);
        wc.environment(World.Environment.NORMAL);
        wc.generateStructures(false);
        return Bukkit.createWorld(wc);
    }

    private void destroyInstanceWorld(ActiveDungeon dungeon) {
        World world = dungeon.getWorld();
        if (world == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                // Kick tat ca player con lai
                for (Player p : world.getPlayers()) teleportToSpawn(p);
                Bukkit.unloadWorld(world, false);
                // Xoa thu muc world
                deleteWorldFolder(world.getWorldFolder());
            }
        }.runTaskLater(plugin, 200L);
    }

    private void deleteWorldFolder(File folder) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteWorldFolder(f);
            else f.delete();
        }
        folder.delete();
    }

    private void removeFromDungeon(UUID uuid, ActiveDungeon dungeon) {
        dungeon.removePlayer(uuid);
        playerDungeonMap.remove(uuid);
        if (dungeon.getPlayers().isEmpty()) {
            activeDungeons.remove(dungeon.getId());
            destroyInstanceWorld(dungeon);
        }
    }

    private void teleportToSpawn(Player player) {
        World overworld = Bukkit.getWorld("world");
        if (overworld != null) player.teleport(overworld.getSpawnLocation());
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        for (ActiveDungeon d : activeDungeons.values()) destroyInstanceWorld(d);
    }

    public ActiveDungeon getDungeon(Player player) {
        return playerDungeonMap.get(player.getUniqueId());
    }
    public boolean inDungeon(Player player) {
        return playerDungeonMap.containsKey(player.getUniqueId());
    }
    public Set<String> getDungeonIds()  { return dungeonDataMap.keySet(); }
    public int         getDungeonCount(){ return dungeonDataMap.size(); }
    public DungeonData getDungeonData(String id) { return dungeonDataMap.get(id); }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
