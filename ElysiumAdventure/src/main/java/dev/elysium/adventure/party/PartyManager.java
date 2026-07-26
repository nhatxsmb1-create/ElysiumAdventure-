package dev.elysium.adventure.party;

import dev.elysium.adventure.ElysiumAdventure;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager {

    private final ElysiumAdventure plugin;

    // UUID player -> Party
    private final Map<UUID, Party> playerPartyMap = new HashMap<>();
    // Party ID -> Party
    private final Map<UUID, Party> parties        = new HashMap<>();

    public PartyManager(ElysiumAdventure plugin) {
        this.plugin = plugin;
    }

    // ── Create / Disband ──────────────────────────────────────────────────────

    public Party createParty(Player leader) {
        Party party = new Party(leader);
        parties.put(party.getId(), party);
        playerPartyMap.put(leader.getUniqueId(), party);
        return party;
    }

    public void disbandParty(Party party) {
        for (UUID m : party.getMembers()) {
            playerPartyMap.remove(m);
            Player p = Bukkit.getPlayer(m);
            if (p != null) p.sendMessage(color("&cParty da bi giai tan."));
        }
        parties.remove(party.getId());
    }

    // ── Invite / Join / Leave ─────────────────────────────────────────────────

    public boolean invite(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null) party = createParty(leader);

        if (!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(color("&cChi truong party moi co the moi!"));
            return false;
        }
        int maxSize = plugin.getAdventureConfig().getPartyMaxSize();
        if (party.getSize() >= maxSize) {
            leader.sendMessage(color("&cParty da day (" + maxSize + " nguoi)!"));
            return false;
        }
        if (party.isMember(target.getUniqueId())) {
            leader.sendMessage(color("&cNguoi choi nay da trong party!"));
            return false;
        }

        int timeout = plugin.getAdventureConfig().getPartyInviteTimeout();
        party.invite(target.getUniqueId(), timeout);

        target.sendMessage(color("&a" + leader.getName() + " &fda moi ban vao party! Dung &e/party accept &fde chap nhan (" + timeout + "s)."));
        leader.sendMessage(color("&aDa gui loi moi toi &f" + target.getName()));
        return true;
    }

    public boolean acceptInvite(Player player) {
        // Tim party co loi moi cho player nay
        for (Party party : parties.values()) {
            if (party.hasPendingInvite(player.getUniqueId())) {
                party.removeInvite(player.getUniqueId());
                party.addMember(player.getUniqueId());
                playerPartyMap.put(player.getUniqueId(), party);
                broadcast(party, "&a" + player.getName() + " &fda tham gia party!");
                return true;
            }
        }
        player.sendMessage(color("&cBan khong co loi moi nao!"));
        return false;
    }

    public void leaveParty(Player player) {
        Party party = getParty(player);
        if (party == null) { player.sendMessage(color("&cBan khong trong party nao!")); return; }

        party.removeMember(player.getUniqueId());
        playerPartyMap.remove(player.getUniqueId());
        player.sendMessage(color("&cBan da roi party."));

        if (party.getSize() == 0) {
            parties.remove(party.getId());
            return;
        }
        // Neu leader roi -> promote nguoi tiep theo
        if (party.isLeader(player.getUniqueId())) {
            party.promoteNext();
            Player newLeader = Bukkit.getPlayer(party.getLeader());
            broadcast(party, "&e" + player.getName() + " &fda roi. Truong party moi: &a"
                    + (newLeader != null ? newLeader.getName() : "Unknown"));
        } else {
            broadcast(party, "&e" + player.getName() + " &fda roi party.");
        }
    }

    public void kickMember(Player leader, Player target) {
        Party party = getParty(leader);
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(color("&cBan khong phai truong party!")); return;
        }
        if (!party.isMember(target.getUniqueId())) {
            leader.sendMessage(color("&cNguoi choi nay khong trong party!")); return;
        }
        party.removeMember(target.getUniqueId());
        playerPartyMap.remove(target.getUniqueId());
        target.sendMessage(color("&cBan da bi kick khoi party."));
        broadcast(party, "&c" + target.getName() + " &fda bi kick khoi party.");
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    public void broadcast(Party party, String message) {
        for (UUID m : party.getMembers()) {
            Player p = Bukkit.getPlayer(m);
            if (p != null) p.sendMessage(color("&5[Party] &r" + message));
        }
    }

    public Party  getParty(Player player)   { return playerPartyMap.get(player.getUniqueId()); }
    public boolean inParty(Player player)   { return playerPartyMap.containsKey(player.getUniqueId()); }
    public void removePlayer(UUID uuid)     { playerPartyMap.remove(uuid); }

    private String color(String s) {
        return s.replace("&", "\u00a7");
    }
}
