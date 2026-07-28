package dev.elysium.adventure.wave;

import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.*;

public class ActiveWaveDungeon {

    public enum State { WAITING, IN_PROGRESS, COUNTDOWN, COMPLETED, FAILED }

    private final UUID                          id;
    private final WaveDungeonData               data;
    private final WaveDungeonData.Difficulty    difficulty;
    private final World                         world;
    private final List<UUID>                    players    = new ArrayList<>();

    private State  state       = State.WAITING;
    private int    currentWave = 0;
    private final Set<UUID> aliveMobs = new HashSet<>();  // UUID entity dang song trong wave
    private final long      startTime;
    private int             countdownSeconds = 0;

    public ActiveWaveDungeon(WaveDungeonData data,
                             WaveDungeonData.Difficulty difficulty,
                             World world) {
        this.id         = UUID.randomUUID();
        this.data       = data;
        this.difficulty = difficulty;
        this.world      = world;
        this.startTime  = System.currentTimeMillis();
    }

    // ── Players ───────────────────────────────────────────────────────────────

    public void addPlayer(UUID uuid)    { players.add(uuid); }
    public void removePlayer(UUID uuid) { players.remove(uuid); }
    public boolean hasPlayer(UUID uuid) { return players.contains(uuid); }

    // ── Wave ──────────────────────────────────────────────────────────────────

    public void nextWave()              { currentWave++; aliveMobs.clear(); }
    public void addAliveMob(UUID uuid)  { aliveMobs.add(uuid); }
    public void removeMob(UUID uuid)    { aliveMobs.remove(uuid); }
    public boolean isWaveClear()        { return aliveMobs.isEmpty(); }
    public boolean isLastWave() {
        return currentWave >= data.getDifficulty(difficulty).getWaveCount();
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    public void startCountdown(int seconds) {
        this.countdownSeconds = seconds;
        this.state = State.COUNTDOWN;
    }
    public int  tickCountdown()         { return --countdownSeconds; }
    public int  getCountdownSeconds()   { return countdownSeconds; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID                          getId()           { return id; }
    public WaveDungeonData               getData()         { return data; }
    public WaveDungeonData.Difficulty    getDifficulty()   { return difficulty; }
    public World                         getWorld()        { return world; }
    public List<UUID>                    getPlayers()      { return Collections.unmodifiableList(players); }
    public State                         getState()        { return state; }
    public void                          setState(State s) { this.state = s; }
    public int                           getCurrentWave()  { return currentWave; }
    public int                           getTotalWaves()   { return data.getDifficulty(difficulty).getWaveCount(); }
    public long                          getStartTime()    { return startTime; }
    public long                          getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
}
