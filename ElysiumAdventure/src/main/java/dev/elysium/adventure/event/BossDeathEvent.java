package dev.elysium.adventure.event;

import dev.elysium.adventure.boss.ActiveBoss;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BossDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ActiveBoss boss;

    public BossDeathEvent(ActiveBoss boss) { this.boss = boss; }

    public ActiveBoss getBoss() { return boss; }

    @Override public HandlerList getHandlers()            { return HANDLERS; }
    public static HandlerList    getHandlerList()         { return HANDLERS; }
}
