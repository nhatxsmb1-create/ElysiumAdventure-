package dev.elysium.adventure.event;

import dev.elysium.adventure.boss.ActiveBoss;
import dev.elysium.adventure.boss.BossData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BossPhaseChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ActiveBoss      boss;
    private final BossData.Phase  newPhase;

    public BossPhaseChangeEvent(ActiveBoss boss, BossData.Phase newPhase) {
        this.boss     = boss;
        this.newPhase = newPhase;
    }

    public ActiveBoss     getBoss()     { return boss; }
    public BossData.Phase getNewPhase() { return newPhase; }

    @Override public HandlerList getHandlers()    { return HANDLERS; }
    public static HandlerList    getHandlerList() { return HANDLERS; }
}
