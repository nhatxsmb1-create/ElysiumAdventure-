package dev.elysium.adventure.command;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.dungeon.DungeonData;
import dev.elysium.adventure.gui.DungeonGui;
import dev.elysium.adventure.gui.GuiListener;
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

        // /dungeon hoac /dungeon menu -> mo GUI
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            DungeonGui gui = new DungeonGui(plugin);
            GuiListener.register(player.getUniqueId(), gui);
            gui.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list", "ds" -> {
                player.sendMessage(color("&5=== Danh sach Dungeon ==="));
                for (String id : plugin.getDungeonManager().getDungeonIds()) {
                    DungeonData d = plugin.getDungeonManager().getDungeonData(id);
                    player.sendMessage(color("  &e" + id + " &f- " + d.getDisplayName()
                            + " &7(Level " + d.getMinLevel() + "+)"));
                }
            }
            case "enter", "vao" -> {
                if (args.length < 2) { player.sendMessage(color("&cDung: /dungeon enter <id>")); return true; }
                plugin.getDungeonManager().enterDungeon(player, args[1].toUpperCase());
            }
            case "leave", "roi" -> plugin.getDungeonManager().leaveDungeon(player);
            default -> {
                player.sendMessage(color("&5=== Dungeon ==="));
                player.sendMessage(color("  &7/dungeon &f- Mo menu dungeon"));
                player.sendMessage(color("  &7/dungeon enter <id> &f- Vao dungeon"));
                player.sendMessage(color("  &7/dungeon leave &f- Roi dungeon"));
            }
        }
        return true;
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
