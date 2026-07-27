package dev.elysium.adventure.dungeon;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.boss.ActiveBoss;
import dev.elysium.adventure.party.Party;
import dev.elysium.adventure.util.AnnounceUtil;
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

    private final Map<String, DungeonData>   dungeonDataMap   = new HashMap<>();
    private final Map<UUID, ActiveDungeon>   playerDungeonMap = new HashMap<>();
    private final Map<UUID, ActiveDungeon>   activeDungeons   = new HashMap<>();

    // Player UUID -> Map<dungeonId, cooldown expire millis>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

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
                    id, d.getString("display-name", id), d.getString("description", ""),
                    d.getInt("min-players", 1), d.getInt("max-players", 6),
                    d.getInt("min-level", 1), d.getInt("time-limit", 1800),
                    d.getInt("cooldown", 3600), d.getString("boss", ""),
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
        if (isOnCooldown(player, dungeonId)) {
            long rem = getCooldownRemaining(player, dungeonId);
            player.sendMessage(color("&cCooldown: &e" + AnnounceUtil.formatTime(rem))); return;
        }

        // Level check
        try {
            int level = dev.elysium.core.api.CoreAPI.getPlayer(player).getLevel();
            if (level < data.getMinLevel()) {
                player.sendMessage(color("&cCan level &e" + data.getMinLevel()
                        + " &cde vao! (Hien tai: " + level + ")")); return;
            }
        } catch (Exception ignored) {}

        Party party = plugin.getPartyManager().getParty(player);
        List<UUID> members = party != null
                ? new ArrayList<>(party.getMembers())
                : List.of(player.getUniqueId());

        if (members.size() < data.getMinPlayers()) {
            player.sendMessage(color("&cCan it nhat &e" + data.getMinPlayers() + " &cnguoi!")); return;
        }
        if (members.size() > data.getMaxPlayers()) {
            player.sendMessage(color("&cToi da &e" + data.getMaxPlayers() + " &cnguoi!")); return;
        }

        // Tao world instance
        World world = createInstanceWorld(dungeonId + "_" + System.currentTimeMillis());
        ActiveDungeon dungeon = new ActiveDungeon(data, world);
        activeDungeons.put(dungeon.getId(), dungeon);

        Location spawnLoc = new Location(world, data.getSpawnX(), data.getSpawnY(),
                data.getSpawnZ(), data.getSpawnYaw(), 0);

        List<Player> memberPlayers = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            dungeon.addPlayer(uuid);
            playerDungeonMap.put(uuid, dungeon);
            p.teleport(spawnLoc);
            memberPlayers.add(p);
        }

        dungeon.setState(ActiveDungeon.State.IN_PROGRESS);

        // Announce enter
        AnnounceUtil.dungeonEnter(memberPlayers, data.getDisplayName());

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
        for (ActiveDungeon dungeon : activeDungeons.values()) {
            if (dungeon.getBoss() != null && dungeon.getBoss().getId().equals(boss.getId())) {
                completeDungeon(dungeon);
                return;
            }
        }
    }

    private void completeDungeon(ActiveDungeon dungeon) {
        dungeon.complete();

        // Tinh score theo thoi gian
        long elapsedSeconds = (System.currentTimeMillis() - dungeon.getStartTime()) / 1000;
        long timeLimit      = dungeon.getData().getTimeLimitSeconds();
        int  score          = calculateScore(elapsedSeconds, timeLimit);
        String timeStr      = AnnounceUtil.formatTime(elapsedSeconds);

        List<Player> players = new ArrayList<>();
        for (UUID uuid : dungeon.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                players.add(p);
                // Bonus EXP theo score
                int bonusExp = score * 10;
                try { dev.elysium.core.api.CoreAPI.addExp(p, bonusExp); } catch (Exception ignored) {}
                p.sendMessage(color("&5[Score] &fDiem: &e" + score + " &7| Bonus EXP: &a+" + bonusExp));
            }
            cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                    .put(dungeon.getData().getId(),
                         System.currentTimeMillis() + dungeon.getData().getCooldownSeconds() * 1000L);
            playerDungeonMap.remove(uuid);
        }

        // Announce complete
        AnnounceUtil.dungeonComplete(players, dungeon.getData().getDisplayName(), timeStr);

        destroyInstanceWorld(dungeon);
        activeDungeons.remove(dungeon.getId());
    }

    /** Score 0-100 — clear nhanh duoc diem cao */
    private int calculateScore(long elapsedSeconds, long timeLimit) {
        if (elapsedSeconds <= 0) return 100;
        double ratio = (double) elapsedSeconds / timeLimit;
        if (ratio <= 0.25) return 100;
        if (ratio <= 0.50) return 80;
        if (ratio <= 0.75) return 60;
        if (ratio <= 0.90) return 40;
        return 20;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private void startTick() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (ActiveDungeon dungeon : new ArrayList<>(activeDungeons.values())) {
                    if (dungeon.isTimedOut()) {
                        List<Player> players = new ArrayList<>();
                        for (UUID uuid : dungeon.getPlayers()) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) { players.add(p); teleportToSpawn(p); }
                            playerDungeonMap.remove(uuid);
                        }
                        AnnounceUtil.dungeonFail(players, "Het thoi gian!");
                        dungeon.fail();
                        destroyInstanceWorld(dungeon);
                        activeDungeons.remove(dungeon.getId());
                    }
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    // ── World ─────────────────────────────────────────────────────────────────

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
            @Override public void run() {
                for (Player p : world.getPlayers()) teleportToSpawn(p);
                Bukkit.unloadWorld(world, false);
                deleteWorldFolder(world.getWorldFolder());
            }
        }.runTaskLater(plugin, 200L);
    }

    private void deleteWorldFolder(File folder) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteWorldFolder(f); else f.delete();
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

    // ── Cooldown ─────────────────────────────────────────────────────────────

    public boolean isOnCooldown(Player player, String dungeonId) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return false;
        Long expire = map.get(dungeonId);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) { map.remove(dungeonId); return false; }
        return true;
    }

    public long getCooldownRemaining(Player player, String dungeonId) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return 0;
        Long expire = map.get(dungeonId);
        if (expire == null) return 0;
        return Math.max(0, (expire - System.currentTimeMillis()) / 1000);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        for (ActiveDungeon d : activeDungeons.values()) destroyInstanceWorld(d);
    }

    public ActiveDungeon getDungeon(Player player)    { return playerDungeonMap.get(player.getUniqueId()); }
    public boolean       inDungeon(Player player)     { return playerDungeonMap.containsKey(player.getUniqueId()); }
    public Set<String>   getDungeonIds()              { return dungeonDataMap.keySet(); }
    public int           getDungeonCount()            { return dungeonDataMap.size(); }
    public DungeonData   getDungeonData(String id)    { return dungeonDataMap.get(id); }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
