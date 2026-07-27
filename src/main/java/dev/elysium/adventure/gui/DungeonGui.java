package dev.elysium.adventure.gui;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.dungeon.DungeonData;
import dev.elysium.core.gui.ElysiumGui;
import dev.elysium.core.gui.GuiButton;
import dev.elysium.core.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DungeonGui extends ElysiumGui {

    private final ElysiumAdventure plugin;

    // Material dai dien cho tung dungeon (theo thu tu)
    private static final Material[] ICONS = {
        Material.NETHER_STAR, Material.DRAGON_EGG, Material.BEACON,
        Material.END_CRYSTAL, Material.CONDUIT
    };

    public DungeonGui(ElysiumAdventure plugin) {
        super("&5&lDungeon", 54);
        this.plugin = plugin;
    }

    @Override
    public void build(Player player) {
        // Fill vien
        fillBorder();

        List<String> ids = new ArrayList<>(plugin.getDungeonManager().getDungeonIds());
        int[] slots = {10, 12, 14, 16, 20, 22, 24, 30, 32};

        for (int i = 0; i < ids.size() && i < slots.length; i++) {
            String      id   = ids.get(i);
            DungeonData data = plugin.getDungeonManager().getDungeonData(id);
            Material    mat  = i < ICONS.length ? ICONS[i] : Material.NETHER_STAR;

            // Kiem tra cooldown
            boolean onCooldown = plugin.getDungeonManager().isOnCooldown(player, id);
            long    remaining  = plugin.getDungeonManager().getCooldownRemaining(player, id);

            List<String> lore = new ArrayList<>();
            lore.add("&7" + data.getDescription());
            lore.add("");
            lore.add("&7Level toi thieu: &e" + data.getMinLevel());
            lore.add("&7So nguoi: &e" + data.getMinPlayers() + " - " + data.getMaxPlayers());
            lore.add("&7Thoi gian: &e" + (data.getTimeLimitSeconds() / 60) + " phut");
            lore.add("&7Boss: &c" + data.getBossId());
            lore.add("");

            ItemStack icon;
            if (onCooldown) {
                lore.add("&cCooldown: &f" + formatTime(remaining));
                icon = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                        .name(data.getDisplayName())
                        .lore(lore)
                        .hideFlags()
                        .build();
            } else {
                lore.add("&aClick de vao dungeon!");
                icon = new ItemBuilder(mat)
                        .name(data.getDisplayName())
                        .lore(lore)
                        .glow()
                        .hideFlags()
                        .build();
            }

            final String dungeonId = id;
            setButton(slots[i], new GuiButton(icon, e -> {
                e.setCancelled(true);
                player.closeInventory();
                plugin.getDungeonManager().enterDungeon(player, dungeonId);
            }));
        }

        // Nut dong
        setButton(49, new GuiButton(
                new ItemBuilder(Material.BARRIER).name("&cDong").build(),
                e -> { e.setCancelled(true); player.closeInventory(); }
        ));
    }

    private void fillBorder() {
        ItemStack border = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
                .name("&r").hideFlags().build();
        int[] borderSlots = {
            0,1,2,3,4,5,6,7,8,
            9,17,18,26,27,35,36,44,
            45,46,47,48,49,50,51,52,53
        };
        for (int s : borderSlots) fill(s, border);
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        if (seconds >= 60)   return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
