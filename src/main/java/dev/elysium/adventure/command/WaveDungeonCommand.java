package dev.elysium.adventure.command;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.gui.GuiListener;
import dev.elysium.adventure.gui.WaveDungeonGui;
import dev.elysium.adventure.wave.WaveDungeonData;
import dev.elysium.adventure.wave.WaveDungeonData.Difficulty;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class WaveDungeonCommand implements CommandExecutor {

    private final ElysiumAdventure plugin;

    public WaveDungeonCommand(ElysiumAdventure plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chi player dung duoc!"); return true;
        }

        // /wavedungeon hoac /wd -> mo GUI
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            WaveDungeonGui gui = new WaveDungeonGui(plugin);
            GuiListener.register(player.getUniqueId(), gui);
            gui.open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enter", "vao" -> {
                if (args.length < 3) {
                    player.sendMessage(color("&cDung: /wd enter <id> <NORMAL|HARD|NIGHTMARE>"));
                    return true;
                }
                Difficulty diff;
                try { diff = Difficulty.valueOf(args[2].toUpperCase()); }
                catch (Exception e) {
                    player.sendMessage(color("&cDo kho phai la: NORMAL, HARD, NIGHTMARE"));
                    return true;
                }
                plugin.getWaveManager().enterDungeon(player, args[1].toUpperCase(), diff);
            }
            case "leave", "roi" -> plugin.getWaveManager().leaveDungeon(player);
            case "list" -> {
                player.sendMessage(color("&5=== Wave Dungeons ==="));
                for (String id : plugin.getWaveManager().getDungeonIds()) {
                    WaveDungeonData d = plugin.getWaveManager().getDungeonData(id);
                    player.sendMessage(color("  &e" + id + " &f- " + d.getDisplayName()));
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(color("&5=== Wave Dungeon ==="));
        p.sendMessage(color("  &7/wd &f- Mo menu"));
        p.sendMessage(color("  &7/wd enter <id> <do kho> &f- Vao dungeon"));
        p.sendMessage(color("  &7/wd leave &f- Roi dungeon"));
        p.sendMessage(color("  &7/wd list &f- Danh sach dungeon"));
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
