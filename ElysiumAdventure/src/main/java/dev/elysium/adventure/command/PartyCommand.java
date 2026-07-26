package dev.elysium.adventure.command;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.party.Party;
import dev.elysium.adventure.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PartyCommand implements CommandExecutor {

    private final ElysiumAdventure plugin;
    private final PartyManager     pm;

    public PartyCommand(ElysiumAdventure plugin) {
        this.plugin = plugin;
        this.pm     = plugin.getPartyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chi player dung duoc!"); return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "invite", "moi" -> {
                if (args.length < 2) { player.sendMessage(color("&cDung: /party invite <ten>")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { player.sendMessage(color("&cKhong tim thay player!")); return true; }
                pm.invite(player, target);
            }
            case "accept", "chap" -> pm.acceptInvite(player);
            case "leave", "roi"   -> pm.leaveParty(player);
            case "kick"           -> {
                if (args.length < 2) { player.sendMessage(color("&cDung: /party kick <ten>")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { player.sendMessage(color("&cKhong tim thay player!")); return true; }
                pm.kickMember(player, target);
            }
            case "disband", "giaitan" -> {
                Party party = pm.getParty(player);
                if (party == null) { player.sendMessage(color("&cBan khong trong party nao!")); return true; }
                if (!party.isLeader(player.getUniqueId())) {
                    player.sendMessage(color("&cChi truong party moi co the giai tan!")); return true;
                }
                pm.disbandParty(party);
            }
            case "info", "i" -> {
                Party party = pm.getParty(player);
                if (party == null) { player.sendMessage(color("&cBan khong trong party nao!")); return true; }
                player.sendMessage(color("&5=== Party Info ==="));
                for (java.util.UUID uuid : party.getMembers()) {
                    Player m = Bukkit.getPlayer(uuid);
                    String name = m != null ? m.getName() : uuid.toString().substring(0, 8);
                    String tag  = party.isLeader(uuid) ? " &e[Leader]" : "";
                    player.sendMessage(color("  &f" + name + tag));
                }
                player.sendMessage(color("  &7Size: &f" + party.getSize()
                        + "/" + plugin.getAdventureConfig().getPartyMaxSize()));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(color("&5=== Party ==="));
        p.sendMessage(color("  &7/party invite <ten> &f- Moi nguoi choi"));
        p.sendMessage(color("  &7/party accept &f- Chap nhan loi moi"));
        p.sendMessage(color("  &7/party leave &f- Roi party"));
        p.sendMessage(color("  &7/party kick <ten> &f- Kick thanh vien"));
        p.sendMessage(color("  &7/party disband &f- Giai tan party"));
        p.sendMessage(color("  &7/party info &f- Xem thong tin party"));
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
