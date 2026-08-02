package dev.elysium.adventure.wave;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.party.Party;
import dev.elysium.adventure.util.AnnounceUtil;
import dev.elysium.adventure.wave.WaveDungeonData.Difficulty;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class WaveManager {

    private final ElysiumAdventure plugin;

    private final Map<String, WaveDungeonData> dungeonDataMap   = new HashMap<>();
    private final Map<UUID, ActiveWaveDungeon> playerDungeonMap = new HashMap<>();
    private final Map<UUID, ActiveWaveDungeon> activeDungeons   = new HashMap<>();
    // Player UUID -> Map<dungeonId_difficulty, cooldown expire>
    private final Map<UUID, Map<String, Long>> cooldowns        = new HashMap<>();

    private BukkitTask tickTask;

    public WaveManager(ElysiumAdventure plugin) {
        this.plugin = plugin;
        loadConfig();
        startTick();
    }

    // ── Config ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        File f = new File(plugin.getDataFolder(), "waves.yml");
        if (!f.exists()) { plugin.saveResource("waves.yml", false); }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection root = cfg.getConfigurationSection("wave-dungeons");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection d = root.getConfigurationSection(id);
            if (d == null) continue;

            // Mob spawn points
            List<WaveDungeonData.MobSpawnPoint> spawnPoints = new ArrayList<>();
            for (Map<?, ?> raw : d.getMapList("mob-spawn")) {
                Map<String, Object> sp = (Map<String, Object>) raw;
                spawnPoints.add(new WaveDungeonData.MobSpawnPoint(
                        ((Number) sp.get("x")).doubleValue(),
                        ((Number) sp.get("y")).doubleValue(),
                        ((Number) sp.get("z")).doubleValue()
                ));
            }

            // Difficulties
            Map<Difficulty, WaveDungeonData.DifficultyConfig> diffMap = new HashMap<>();
            ConfigurationSection diffSec = d.getConfigurationSection("difficulties");
            if (diffSec != null) {
                for (String diffName : diffSec.getKeys(false)) {
                    Difficulty diff;
                    try { diff = Difficulty.valueOf(diffName.toUpperCase()); }
                    catch (Exception e) { continue; }

                    ConfigurationSection dc = diffSec.getConfigurationSection(diffName);
                    if (dc == null) continue;

                    // Reward
                    ConfigurationSection rSec = dc.getConfigurationSection("rewards");
                    WaveDungeonData.WaveReward reward = new WaveDungeonData.WaveReward(
                            rSec != null ? rSec.getInt("exp", 0)   : 0,
                            rSec != null ? rSec.getInt("money", 0) : 0,
                            rSec != null ? rSec.getStringList("commands") : new ArrayList<>()
                    );

                    // Waves
                    Map<Integer, List<WaveDungeonData.WaveMob>> wavesMap = new HashMap<>();
                    ConfigurationSection wavesSec = dc.getConfigurationSection("waves");
                    if (wavesSec != null) {
                        for (String waveNum : wavesSec.getKeys(false)) {
                            int waveNumber;
                            try { waveNumber = Integer.parseInt(waveNum); }
                            catch (NumberFormatException e) { continue; }

                            List<WaveDungeonData.WaveMob> mobList = new ArrayList<>();
                            for (Map<?, ?> mobRaw : wavesSec.getMapList(waveNum)) {
                                Map<String, Object> mob = (Map<String, Object>) mobRaw;
                                mobList.add(new WaveDungeonData.WaveMob(
                                        (String) mob.get("mob"),
                                        mob.containsKey("level") ? ((Number) mob.get("level")).intValue() : 1,
                                        mob.containsKey("count") ? ((Number) mob.get("count")).intValue() : 1
                                ));
                            }
                            wavesMap.put(waveNumber, mobList);
                        }
                    }

                    diffMap.put(diff, new WaveDungeonData.DifficultyConfig(
                            dc.getInt("wave-count", 5),
                            dc.getInt("wave-delay", 10),
                            reward, wavesMap
                    ));
                }
            }

            ConfigurationSection spawnSec = d.getConfigurationSection("spawn");
            dungeonDataMap.put(id, new WaveDungeonData(
                    id,
                    d.getString("display-name", id),
                    d.getString("description", ""),
                    d.getString("world-template", ""),
                    d.getInt("min-players", 1),
                    d.getInt("max-players", 6),
                    spawnSec != null ? spawnSec.getDouble("x") : 0,
                    spawnSec != null ? spawnSec.getDouble("y") : 64,
                    spawnSec != null ? spawnSec.getDouble("z") : 0,
                    spawnSec != null ? (float) spawnSec.getDouble("yaw") : 0,
                    spawnPoints, diffMap
            ));
        }
        plugin.getLogger().info("Loaded " + dungeonDataMap.size() + " wave dungeon(s).");
    }

    // ── Enter ─────────────────────────────────────────────────────────────────

    public void enterDungeon(Player player, String dungeonId, Difficulty difficulty) {
        WaveDungeonData data = dungeonDataMap.get(dungeonId);
        if (data == null) { player.sendMessage(color("&cDungeon khong ton tai!")); return; }
        if (!data.hasDifficulty(difficulty)) {
            player.sendMessage(color("&cDo kho nay chua duoc mo!")); return;
        }
        if (playerDungeonMap.containsKey(player.getUniqueId())) {
            player.sendMessage(color("&cBan dang trong dungeon roi!")); return;
        }

        String cdKey = dungeonId + "_" + difficulty.name();
        if (isOnCooldown(player, cdKey)) {
            long rem = getCooldownRemaining(player, cdKey);
            player.sendMessage(color("&cCooldown: &e" + AnnounceUtil.formatTime(rem))); return;
        }

        Party party = plugin.getPartyManager().getParty(player);
        List<UUID> members = party != null
                ? new ArrayList<>(party.getMembers())
                : List.of(player.getUniqueId());

        if (members.size() < data.getMinPlayers()) {
            player.sendMessage(color("&cCan it nhat &e" + data.getMinPlayers() + " &cnguoi!")); return;
        }

        // Tao world
        World world = createWorld(dungeonId + "_" + difficulty.name() + "_" + System.currentTimeMillis());
        ActiveWaveDungeon dungeon = new ActiveWaveDungeon(data, difficulty, world);
        activeDungeons.put(dungeon.getId(), dungeon);

        Location spawnLoc = new Location(world,
                data.getSpawnX(), data.getSpawnY(), data.getSpawnZ(), data.getSpawnYaw(), 0);

        List<Player> memberPlayers = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            dungeon.addPlayer(uuid);
            playerDungeonMap.put(uuid, dungeon);
            p.teleport(spawnLoc);
            memberPlayers.add(p);
        }

        dungeon.setState(ActiveWaveDungeon.State.IN_PROGRESS);

        // Announce
        String diffColor = switch (difficulty) {
            case NORMAL    -> "&a";
            case HARD      -> "&6";
            case NIGHTMARE -> "&4";
        };
        for (Player p : memberPlayers) {
            AnnounceUtil.title(p,
                    diffColor + "&l" + data.getDisplayName(),
                    "&f" + difficulty.name() + " &7| " + data.getDifficulty(difficulty).getWaveCount() + " waves");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
        }

        // Bat dau wave 1
        startNextWave(dungeon);
    }

    // ── Wave Logic ────────────────────────────────────────────────────────────

    private void startNextWave(ActiveWaveDungeon dungeon) {
        dungeon.nextWave();
        int waveNumber = dungeon.getCurrentWave();
        int totalWaves = dungeon.getTotalWaves();
        WaveDungeonData.DifficultyConfig dc = dungeon.getData().getDifficulty(dungeon.getDifficulty());
        List<WaveDungeonData.WaveMob> mobs  = dc.getWave(waveNumber);

        // Broadcast wave info
        List<Player> players = getOnlinePlayers(dungeon);
        for (Player p : players) {
            p.sendMessage(color("&5&l[Wave " + waveNumber + "/" + totalWaves + "] &fBat dau!"));
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.2f);

            // Title cho wave cuoi
            if (waveNumber == totalWaves) {
                AnnounceUtil.title(p, "&4&l⚠ WAVE CUOI!", "&fTieu diet tat ca de thang!", 5, 40, 10);
            } else {
                AnnounceUtil.title(p, "&e&lWave " + waveNumber + "/" + totalWaves, "&fTieu diet tat ca ke thu!", 5, 30, 10);
            }
        }

        // Spawn mobs
        spawnWaveMobs(dungeon, mobs);
    }

    private void spawnWaveMobs(ActiveWaveDungeon dungeon, List<WaveDungeonData.WaveMob> mobs) {
        World world = dungeon.getWorld();
        List<WaveDungeonData.MobSpawnPoint> spawnPoints = dungeon.getData().getMobSpawnPoints();
        if (spawnPoints.isEmpty()) return;

        boolean mythicEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
        Random rng = new Random();
        int spawnIndex = 0;

        for (WaveDungeonData.WaveMob waveMob : mobs) {
            for (int i = 0; i < waveMob.getCount(); i++) {
                // Round-robin spawn points
                WaveDungeonData.MobSpawnPoint sp = spawnPoints.get(spawnIndex % spawnPoints.size());
                spawnIndex++;

                Location loc = new Location(world, sp.x, sp.y, sp.z);

                // Them random offset nho de mob khong stack len nhau
                loc.add((rng.nextDouble() - 0.5) * 2, 0, (rng.nextDouble() - 0.5) * 2);

                if (mythicEnabled) {
                    try {
                        var abstractLoc = BukkitAdapter.adapt(loc);
                        var mythicMob   = MythicBukkit.inst().getMobManager()
                                .spawnMob(waveMob.getMythicMobId(), abstractLoc, waveMob.getLevel());
                        Entity entity = (Entity) mythicMob.getEntity().getBukkitEntity();
                        dungeon.addAliveMob(entity.getUniqueId());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Khong spawn duoc mob: "
                                + waveMob.getMythicMobId() + " - " + e.getMessage());
                    }
                } else {
                    // Fallback: spawn zombie
                    LivingEntity entity = (LivingEntity) world.spawnEntity(loc, org.bukkit.entity.EntityType.ZOMBIE);
                    entity.setCustomName(color("&c" + waveMob.getMythicMobId()));
                    entity.setCustomNameVisible(true);
                    dungeon.addAliveMob(entity.getUniqueId());
                }
            }
        }
    }

    /** Goi tu WaveListener khi mob trong dungeon bi giet */
    public void onMobDeath(UUID entityUuid) {
        for (ActiveWaveDungeon dungeon : activeDungeons.values()) {
            if (!dungeon.getWorld().getEntities().stream()
                    .map(Entity::getUniqueId)
                    .anyMatch(u -> u.equals(entityUuid))) continue;

            dungeon.removeMob(entityUuid);

            List<Player> players = getOnlinePlayers(dungeon);
            int alive = (int) dungeon.getWorld().getEntities().stream()
                    .filter(e -> e instanceof LivingEntity && !(e instanceof Player)).count();

            // Thong bao so quai con lai
            for (Player p : players) {
                p.sendMessage(color("&7Ke thu con lai: &e" + alive));
            }

            if (dungeon.isWaveClear()) {
                if (dungeon.isLastWave()) {
                    completeDungeon(dungeon);
                } else {
                    // Dem nguoc truoc khi bat dau wave tiep theo
                    int delay = dungeon.getData().getDifficulty(dungeon.getDifficulty()).getWaveDelay();
                    dungeon.startCountdown(delay);

                    for (Player p : players) {
                        p.sendMessage(color("&a&l[Wave Clear!] &fWave tiep theo sau &e" + delay + "s"));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                    }
                }
            }
            break;
        }
    }

    // ── Complete / Fail ───────────────────────────────────────────────────────

    private void completeDungeon(ActiveWaveDungeon dungeon) {
        dungeon.setState(ActiveWaveDungeon.State.COMPLETED);
        String timeStr = AnnounceUtil.formatTime(dungeon.getElapsedSeconds());
        WaveDungeonData.WaveReward reward = dungeon.getData()
                .getDifficulty(dungeon.getDifficulty()).getReward();

        List<Player> players = getOnlinePlayers(dungeon);
        AnnounceUtil.dungeonComplete(players, dungeon.getData().getDisplayName(), timeStr);

        for (UUID uuid : dungeon.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                giveReward(p, reward);
                teleportToSpawn(p);
            }
            // Set cooldown 30 phut
            cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                    .put(dungeon.getData().getId() + "_" + dungeon.getDifficulty().name(),
                         System.currentTimeMillis() + 30 * 60 * 1000L);
            playerDungeonMap.remove(uuid);
        }

        destroyWorld(dungeon);
        activeDungeons.remove(dungeon.getId());
    }

    private void failDungeon(ActiveWaveDungeon dungeon, String reason) {
        dungeon.setState(ActiveWaveDungeon.State.FAILED);
        List<Player> players = getOnlinePlayers(dungeon);
        AnnounceUtil.dungeonFail(players, reason);

        for (UUID uuid : dungeon.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) teleportToSpawn(p);
            playerDungeonMap.remove(uuid);
        }
        destroyWorld(dungeon);
        activeDungeons.remove(dungeon.getId());
    }

    private void giveReward(Player player, WaveDungeonData.WaveReward reward) {
        try { dev.elysium.core.api.CoreAPI.addExp(player, reward.getExp()); } catch (Exception ignored) {}
        dev.elysium.core.api.CoreAPI.addBalance(player, reward.getMoney());
        for (String cmd : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        // Hook Weapon EXP - Wave Dungeon (x1.0, nhieu wave hon nen it hon moi lan)
        giveWeaponExp(player, 150L, "DUNGEON");

        player.sendMessage(color("&5[Reward] &e+" + reward.getExp() + " EXP &f| &a+" + reward.getMoney() + " coin"));
    }

    /** Hook Weapon EXP qua reflection */
    private void giveWeaponExp(Player player, long amount, String source) {
        try {
            Class<?> api = Class.forName("dev.elysium.weapon.api.WeaponAPI");
            String weaponId = (String) api.getMethod("getHeldWeaponId", org.bukkit.entity.Player.class)
                    .invoke(null, player);
            if (weaponId != null) {
                api.getMethod("addWeaponExp", org.bukkit.entity.Player.class, String.class, long.class, String.class)
                        .invoke(null, player, weaponId, amount, source);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            plugin.getLogger().warning("[Adventure] WeaponEXP error: " + e.getMessage());
        }
    }

    // ── Tick (dem nguoc giua cac wave) ────────────────────────────────────────

    private void startTick() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (ActiveWaveDungeon dungeon : new ArrayList<>(activeDungeons.values())) {
                    if (dungeon.getState() != ActiveWaveDungeon.State.COUNTDOWN) continue;

                    int remaining = dungeon.tickCountdown();
                    List<Player> players = getOnlinePlayers(dungeon);

                    if (remaining <= 0) {
                        dungeon.setState(ActiveWaveDungeon.State.IN_PROGRESS);
                        startNextWave(dungeon);
                    } else if (remaining <= 5) {
                        // Dem nguoc 5-1
                        for (Player p : players) {
                            p.sendTitle(color("&e" + remaining), "", 0, 25, 5);
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // moi 1 giay
    }

    // ── World ─────────────────────────────────────────────────────────────────

    private World createWorld(String name) {
        WorldCreator wc = new WorldCreator(name);
        wc.environment(World.Environment.NORMAL);
        wc.generateStructures(false);
        return Bukkit.createWorld(wc);
    }

    private void destroyWorld(ActiveWaveDungeon dungeon) {
        World world = dungeon.getWorld();
        if (world == null) return;
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : world.getPlayers()) teleportToSpawn(p);
                Bukkit.unloadWorld(world, false);
                deleteFolder(world.getWorldFolder());
            }
        }.runTaskLater(plugin, 200L);
    }

    private void deleteFolder(File folder) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteFolder(f); else f.delete();
        }
        folder.delete();
    }

    private void teleportToSpawn(Player player) {
        World w = Bukkit.getWorld("world");
        if (w != null) player.teleport(w.getSpawnLocation());
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private List<Player> getOnlinePlayers(ActiveWaveDungeon dungeon) {
        List<Player> list = new ArrayList<>();
        for (UUID uuid : dungeon.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) list.add(p);
        }
        return list;
    }

    public boolean isOnCooldown(Player player, String cdKey) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return false;
        Long expire = map.get(cdKey);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) { map.remove(cdKey); return false; }
        return true;
    }

    public long getCooldownRemaining(Player player, String cdKey) {
        Map<String, Long> map = cooldowns.get(player.getUniqueId());
        if (map == null) return 0;
        Long expire = map.get(cdKey);
        if (expire == null) return 0;
        return Math.max(0, (expire - System.currentTimeMillis()) / 1000);
    }

    public ActiveWaveDungeon getDungeon(Player player) {
        return playerDungeonMap.get(player.getUniqueId());
    }
    public boolean inDungeon(Player player) {
        return playerDungeonMap.containsKey(player.getUniqueId());
    }
    public Set<String> getDungeonIds()             { return dungeonDataMap.keySet(); }
    public WaveDungeonData getDungeonData(String id) { return dungeonDataMap.get(id); }

    public ActiveWaveDungeon getDungeonByWorld(org.bukkit.World world) {
        for (ActiveWaveDungeon d : activeDungeons.values()) {
            if (d.getWorld() != null && d.getWorld().equals(world)) return d;
        }
        return null;
    }

    public void leaveDungeon(Player player) {
        ActiveWaveDungeon dungeon = playerDungeonMap.get(player.getUniqueId());
        if (dungeon == null) { player.sendMessage(color("&cBan khong trong dungeon nao!")); return; }
        dungeon.removePlayer(player.getUniqueId());
        playerDungeonMap.remove(player.getUniqueId());
        teleportToSpawn(player);
        player.sendMessage(color("&cBan da roi dungeon."));
        if (dungeon.getPlayers().isEmpty()) {
            activeDungeons.remove(dungeon.getId());
            destroyWorld(dungeon);
        }
    }

    public void onPlayerQuit(Player player) {
        ActiveWaveDungeon dungeon = playerDungeonMap.get(player.getUniqueId());
        if (dungeon == null) return;
        dungeon.removePlayer(player.getUniqueId());
        playerDungeonMap.remove(player.getUniqueId());
        if (dungeon.getPlayers().isEmpty()) {
            activeDungeons.remove(dungeon.getId());
            destroyWorld(dungeon);
        }
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        for (ActiveWaveDungeon d : activeDungeons.values()) destroyWorld(d);
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}

// Extension methods appended
