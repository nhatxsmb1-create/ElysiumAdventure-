package dev.elysium.adventure.party;

import org.bukkit.entity.Player;

import java.util.*;

public class Party {

    private final UUID        id;
    private UUID              leader;
    private final List<UUID>  members   = new ArrayList<>();
    private final Map<UUID, Long> pendingInvites = new HashMap<>(); // UUID -> expire time

    public Party(Player leader) {
        this.id     = UUID.randomUUID();
        this.leader = leader.getUniqueId();
        this.members.add(leader.getUniqueId());
    }

    // ── Invite ────────────────────────────────────────────────────────────────

    public void invite(UUID target, int timeoutSeconds) {
        pendingInvites.put(target, System.currentTimeMillis() + timeoutSeconds * 1000L);
    }

    public boolean hasPendingInvite(UUID target) {
        Long expire = pendingInvites.get(target);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            pendingInvites.remove(target);
            return false;
        }
        return true;
    }

    public void removeInvite(UUID target) {
        pendingInvites.remove(target);
    }

    // ── Members ───────────────────────────────────────────────────────────────

    public void addMember(UUID uuid)    { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public boolean isMember(UUID uuid)  { return members.contains(uuid); }
    public boolean isLeader(UUID uuid)  { return leader.equals(uuid); }

    public void promoteNext() {
        if (members.isEmpty()) return;
        // Promote nguoi tiep theo trong danh sach
        for (UUID m : members) {
            if (!m.equals(leader)) { leader = m; return; }
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID        getId()      { return id; }
    public UUID        getLeader()  { return leader; }
    public List<UUID>  getMembers() { return Collections.unmodifiableList(members); }
    public int         getSize()    { return members.size(); }
}
