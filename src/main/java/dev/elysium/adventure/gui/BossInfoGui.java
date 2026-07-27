package dev.elysium.adventure.gui;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.boss.ActiveBoss;
import dev.elysium.adventure.boss.BossData;
import dev.elysium.core.gui.ElysiumGui;
import dev.elysium.core.gui.GuiButton;
import dev.elysium.core.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BossInfoGui extends ElysiumGui {

    private final ActiveBoss boss;

    public BossInfoGui(ActiveBoss boss) {
        super("&4&lBoss Info", 27);
        this.boss = boss;
    }

    @Override
    public void build(Player player) {
        fill(ItemBuilder.filler(Material.RED_STAINED_GLASS_PANE));

        BossData data  = boss.getData();
        double   hpPct = boss.getHpPercent();

        // HP bar
        String hpBar = buildHpBar(hpPct);

        // Icon boss (slot 13 - center)
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7HP: " + hpBar);
        lore.add("&7    &f" + String.format("%.0f", boss.getCurrentHp())
                + " / " + (int) data.getMaxHp());
        lore.add("");
        lore.add("&7Phase hien tai: &e" + boss.getCurrentPhase().getName());
        lore.add("");
        lore.add("&7Skill dang dung:");
        for (String skillId : boss.getCurrentPhase().getSkills()) {
            lore.add("  &c• &f" + skillId);
        }

        fill(13, new ItemBuilder(Material.WITHER_SKELETON_SKULL)
                .name(data.getDisplayName())
                .lore(lore)
                .build());

        // Reward info (slot 11)
        List<String> rewardLore = new ArrayList<>();
        rewardLore.add("");
        rewardLore.add("&7EXP: &e+" + data.getReward().getExp());
        rewardLore.add("&7Coin: &a+" + data.getReward().getMoney());
        if (!data.getReward().getCommands().isEmpty()) {
            rewardLore.add("&7+ Phan thuong dac biet!");
        }
        fill(11, new ItemBuilder(Material.CHEST)
                .name("&6Phan Thuong")
                .lore(rewardLore)
                .build());

        // Phase info (slot 15)
        List<String> phaseLore = new ArrayList<>();
        phaseLore.add("");
        for (BossData.Phase phase : data.getPhases()) {
            String current = boss.getCurrentPhase().getName().equals(phase.getName())
                    ? " &a◄" : "";
            phaseLore.add("&7" + phase.getThreshold() + "% HP: &f" + phase.getName() + current);
        }
        fill(15, new ItemBuilder(Material.BOOK)
                .name("&bCac Phase")
                .lore(phaseLore)
                .build());

        // Dong (slot 22)
        setButton(22, new GuiButton(
                new ItemBuilder(Material.BARRIER).name("&cDong").build(),
                e -> { e.setCancelled(true); player.closeInventory(); }
        ));
    }

    private String buildHpBar(double pct) {
        int filled = (int) (pct / 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "&c❤" : "&8❤");
        }
        return bar.toString().replace("&", "\u00a7");
    }
}
