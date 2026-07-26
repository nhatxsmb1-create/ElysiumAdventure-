package dev.elysium.adventure.dungeon;

import dev.elysium.adventure.boss.ActiveBoss;
import org.bukkit.World;

import java.util.*;

public class ActiveDungeon {

    public enum State { WAITING, IN_PROGRESS, COMPLETED, FAILED }

    private final UUID        id;
    private final DungeonData data;
    private final World       world;          // World clone rieng
    private final List<UUID>  players = new ArrayList<>();
    private ActiveBoss        boss;
    private State             state   = State.WAITING;
    private final long        startTime;
    private long              endTime = -1;

    public ActiveDungeon(DungeonData data, World world) {
        this.id        = UUID.randomUUID();
        this.data      = data;
        this.world     = world;
        this.startTime = System.currentTimeMillis();
    }

    public void addPlayer(UUID uuid)    { players.add(uuid); }
    public void removePlayer(UUID uuid) { players.remove(uuid); }
    public boolean hasPlayer(UUID uuid) { return players.contains(uuid); }

    public void complete() {
        state   = State.COMPLETED;
        endTime = System.currentTimeMillis();
    }

    public void fail() {
        state   = State.FAILED;
        endTime = System.currentTimeMillis();
    }

    public boolean isTimedOut() {
        if (state != State.IN_PROGRESS) return false;
        return System.currentTimeMillis() - startTime > data.getTimeLimitSeconds() * 1000L;
    }

    public UUID        getId()       { return id; }
    public DungeonData getData()     { return data; }
    public World       getWorld()    { return world; }
    public List<UUID>  getPlayers()  { return Collections.unmodifiableList(players); }
    public ActiveBoss  getBoss()     { return boss; }
    public void        setBoss(ActiveBoss b) { this.boss = b; }
    public State       getState()    { return state; }
    public void        setState(State s) { this.state = s; }
    public long        getStartTime(){ return startTime; }
    public int         getRemainingSeconds() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, data.getTimeLimitSeconds() - (int)(elapsed / 1000));
    }
}
