package dev.elysium.adventure.command;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.dungeon.DungeonData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class DungeonCommand implements CommandExecutor {

    private final ElysiumAdventure plugin;

    public DungeonCommand(ElysiumAdventure plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chi player dung duoc!"); return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "list", "ds" -> {
                player.sendMessage(color("&5=== Danh sach Dungeon ==="));
                for (String id : plugin.getDungeonManager().getDungeonIds()) {
                    DungeonData d = plugin.getDungeonManager().getDungeonData(id);
                    player.sendMessage(color("  &e" + id + " &f- " + d.getDisplayName()
                            + " &7(Level " + d.getMinLevel() + "+ | "
                            + d.getMinPlayers() + "-" + d.getMaxPlayers() + " nguoi)"));
                }
            }
            case "info", "i" -> {
                if (args.length < 2) { player.sendMessage(color("&cDung: /dungeon info <id>")); return true; }
                DungeonData d = plugin.getDungeonManager().getDungeonData(args[1].toUpperCase());
                if (d == null) { player.sendMessage(color("&cDungeon khong ton tai!")); return true; }
                player.sendMessage(color("&5=== " + d.getDisplayName() + " ==="));
                player.sendMessage(color("  &7Mo ta: &f" + d.getDescription()));
                player.sendMessage(color("  &7Level toi thieu: &e" + d.getMinLevel()));
                player.sendMessage(color("  &7So nguoi: &e" + d.getMinPlayers() + "-" + d.getMaxPlayers()));
                player.sendMessage(color("  &7Thoi gian: &e" + (d.getTimeLimitSeconds() / 60) + " phut"));
                player.sendMessage(color("  &7Cooldown: &e" + (d.getCooldownSeconds() / 60) + " phut"));
                player.sendMessage(color("  &7Boss: &c" + d.getBossId()));
            }
            case "enter", "vao" -> {
                if (args.length < 2) { player.sendMessage(color("&cDung: /dungeon enter <id>")); return true; }
                plugin.getDungeonManager().enterDungeon(player, args[1].toUpperCase());
            }
            case "leave", "roi" -> plugin.getDungeonManager().leaveDungeon(player);
            default             -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(color("&5=== Dungeon ==="));
        p.sendMessage(color("  &7/dungeon list &f- Danh sach dungeon"));
        p.sendMessage(color("  &7/dungeon info <id> &f- Thong tin dungeon"));
        p.sendMessage(color("  &7/dungeon enter <id> &f- Vao dungeon"));
        p.sendMessage(color("  &7/dungeon leave &f- Roi dungeon"));
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
