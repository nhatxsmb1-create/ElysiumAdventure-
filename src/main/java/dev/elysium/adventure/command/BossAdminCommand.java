package dev.elysium.adventure.command;

import dev.elysium.adventure.ElysiumAdventure;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class BossAdminCommand implements CommandExecutor {

    private final ElysiumAdventure plugin;

    public BossAdminCommand(ElysiumAdventure plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("elysium.adventure.admin")) {
            sender.sendMessage(color("&cKhong co quyen!")); return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Chi player dung duoc!"); return true;
        }
        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (args.length < 2) { player.sendMessage(color("&cDung: /boss spawn <id>")); return true; }
                String bossId = args[1].toUpperCase();
                var boss = plugin.getBossManager().spawnBoss(bossId, player.getLocation());
                if (boss == null) player.sendMessage(color("&cKhong the spawn boss: " + bossId));
                else player.sendMessage(color("&aSpawn boss &f" + bossId + " &athanh cong!"));
            }
            case "list" -> {
                player.sendMessage(color("&5=== Boss List ==="));
                for (String id : plugin.getBossManager().getBossIds()) {
                    player.sendMessage(color("  &e" + id));
                }
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player p) {
        p.sendMessage(color("&5=== Boss Admin ==="));
        p.sendMessage(color("  &7/boss spawn <id> &f- Spawn boss tai vi tri hien tai"));
        p.sendMessage(color("  &7/boss list &f- Danh sach boss"));
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
