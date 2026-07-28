package dev.elysium.adventure.gui;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.wave.WaveDungeonData;
import dev.elysium.adventure.wave.WaveDungeonData.Difficulty;
import dev.elysium.core.gui.ElysiumGui;
import dev.elysium.core.gui.GuiButton;
import dev.elysium.core.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WaveDungeonGui extends ElysiumGui {

    private final ElysiumAdventure plugin;
    private String selectedDungeon = null;

    public WaveDungeonGui(ElysiumAdventure plugin) {
        super("&5&lWave Dungeon", 54);
        this.plugin = plugin;
    }

    @Override
    public void build(Player player) {
        fill(ItemBuilder.filler());

        if (selectedDungeon == null) {
            buildDungeonList(player);
        } else {
            buildDifficultySelect(player);
        }
    }

    // ── Danh sach dungeon ─────────────────────────────────────────────────────

    private void buildDungeonList(Player player) {
        List<String> ids = new ArrayList<>(plugin.getWaveManager().getDungeonIds());
        int[] slots = {20, 22, 24};

        for (int i = 0; i < ids.size() && i < slots.length; i++) {
            String          id   = ids.get(i);
            WaveDungeonData data = plugin.getWaveManager().getDungeonData(id);

            List<String> lore = new ArrayList<>();
            lore.add("&7" + data.getDescription());
            lore.add("");
            lore.add("&7So nguoi: &f" + data.getMinPlayers() + "-" + data.getMaxPlayers());
            lore.add("");
            lore.add("&aNORMAL &7| &6HARD &7| &4NIGHTMARE");
            lore.add("");
            lore.add("&eClick de chon do kho!");

            setButton(slots[i], new GuiButton(
                    new ItemBuilder(Material.NETHER_STAR)
                            .name(data.getDisplayName())
                            .lore(lore)
                            .glow()
                            .build(),
                    e -> {
                        e.setCancelled(true);
                        selectedDungeon = id;
                        build(player);
                        player.openInventory(getInventory());
                    }
            ));
        }

        // Nut dong
        setButton(49, new GuiButton(
                new ItemBuilder(Material.BARRIER).name("&cDong").build(),
                e -> { e.setCancelled(true); player.closeInventory(); }
        ));
    }

    // ── Chon do kho ───────────────────────────────────────────────────────────

    private void buildDifficultySelect(Player player) {
        WaveDungeonData data = plugin.getWaveManager().getDungeonData(selectedDungeon);

        // Title dungeon (slot 4)
        fill(4, new ItemBuilder(Material.NETHER_STAR)
                .name(data.getDisplayName())
                .lore("&7Chon do kho ben duoi")
                .glow()
                .build());

        // NORMAL (slot 20)
        buildDiffButton(player, 20, Difficulty.NORMAL, data,
                Material.LIME_STAINED_GLASS_PANE, "&a&lNORMAL",
                "&75 Waves | Do kho thap");

        // HARD (slot 22)
        buildDiffButton(player, 22, Difficulty.HARD, data,
                Material.ORANGE_STAINED_GLASS_PANE, "&6&lHARD",
                "&710 Waves | Do kho trung binh");

        // NIGHTMARE (slot 24)
        buildDiffButton(player, 24, Difficulty.NIGHTMARE, data,
                Material.RED_STAINED_GLASS_PANE, "&4&lNIGHTMARE",
                "&715 Waves | Do kho cao nhat");

        // Nut quay lai (slot 45)
        setButton(45, new GuiButton(
                new ItemBuilder(Material.ARROW).name("&7Quay Lai").build(),
                e -> {
                    e.setCancelled(true);
                    selectedDungeon = null;
                    build(player);
                    player.openInventory(getInventory());
                }
        ));

        // Nut dong (slot 49)
        setButton(49, new GuiButton(
                new ItemBuilder(Material.BARRIER).name("&cDong").build(),
                e -> { e.setCancelled(true); player.closeInventory(); }
        ));
    }

    private void buildDiffButton(Player player, int slot, Difficulty diff,
                                  WaveDungeonData data, Material mat,
                                  String name, String desc) {
        boolean onCooldown = plugin.getWaveManager().isOnCooldown(
                player, selectedDungeon + "_" + diff.name());
        long remaining = plugin.getWaveManager().getCooldownRemaining(
                player, selectedDungeon + "_" + diff.name());

        WaveDungeonData.DifficultyConfig dc = data.getDifficulty(diff);

        List<String> lore = new ArrayList<>();
        lore.add("&7" + desc);
        lore.add("");
        if (dc != null) {
            lore.add("&7Waves: &f" + dc.getWaveCount());
            lore.add("&7Delay giua wave: &f" + dc.getWaveDelay() + "s");
            lore.add("&7Reward: &e+" + dc.getReward().getExp() + " EXP &f| &a+" + dc.getReward().getMoney() + " coin");
        }
        lore.add("");

        if (onCooldown) {
            lore.add("&cCooldown: &f" + formatTime(remaining));
            mat = Material.GRAY_STAINED_GLASS_PANE;
        } else {
            lore.add("&aClick de vao!");
        }

        final boolean cd = onCooldown;
        setButton(slot, new GuiButton(
                new ItemBuilder(mat).name(name).lore(lore).build(),
                e -> {
                    e.setCancelled(true);
                    if (cd) return;
                    player.closeInventory();
                    plugin.getWaveManager().enterDungeon(player, selectedDungeon, diff);
                }
        ));
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        if (seconds >= 60)   return (seconds / 60) + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }
}
