package dev.elysium.adventure.command;

import dev.elysium.adventure.ElysiumAdventure;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AdventureCommand implements CommandExecutor {

    private final ElysiumAdventure plugin;

    public AdventureCommand(ElysiumAdventure plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chi player dung duoc!"); return true;
        }
        player.sendMessage(color("&5=== ElysiumAdventure ==="));
        player.sendMessage(color("  &7/party &f- He thong party"));
        player.sendMessage(color("  &7/dungeon &f- He thong dungeon"));
        player.sendMessage(color("  &7/boss &f- Boss admin (OP)"));
        return true;
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
