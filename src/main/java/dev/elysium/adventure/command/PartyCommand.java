package dev.elysium.adventure.command;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.gui.GuiListener;
import dev.elysium.adventure.gui.PartyGui;
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

        // /party hoac /party menu -> mo GUI
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            openGui(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create", "tao" -> {
                if (pm.inParty(player)) {
                    player.sendMessage(color("&cBan da trong party roi!")); return true;
                }
                pm.createParty(player);
                player.sendMessage(color("&aTao party thanh cong! Dung &e/party invite <ten> &ede moi nguoi."));
                openGui(player);
            }
            case "invite", "moi" -> {
                if (args.length < 2) { player.sendMessage(color("&cDung: /party invite <ten>")); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { player.sendMessage(color("&cKhong tim thay player!")); return true; }
                pm.invite(player, target);
            }
            case "accept", "chap" -> pm.acceptInvite(player);
            case "leave", "roi"   -> pm.leaveParty(player);
            case "kick" -> {
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
            default -> sendHelp(player);
        }
        return true;
    }

    private void openGui(Player player) {
        PartyGui gui = new PartyGui(plugin);
        GuiListener.register(player.getUniqueId(), gui);
        gui.open(player);
    }

    private void sendHelp(Player p) {
        p.sendMessage(color("&5=== Party ==="));
        p.sendMessage(color("  &7/party &f- Mo menu party"));
        p.sendMessage(color("  &7/party create &f- Tao party moi"));
        p.sendMessage(color("  &7/party invite <ten> &f- Moi nguoi choi"));
        p.sendMessage(color("  &7/party accept &f- Chap nhan loi moi"));
        p.sendMessage(color("  &7/party leave &f- Roi party"));
        p.sendMessage(color("  &7/party kick <ten> &f- Kick thanh vien"));
        p.sendMessage(color("  &7/party disband &f- Giai tan party"));
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
