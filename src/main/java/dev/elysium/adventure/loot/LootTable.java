package dev.elysium.adventure.loot;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LootTable {

    private final List<LootEntry> entries = new ArrayList<>();
    private final Random rng = new Random();

    public void addEntry(LootEntry entry) {
        entries.add(entry);
    }

    /** Drop tat ca item trung qua RNG tai location */
    public List<ItemStack> roll() {
        List<ItemStack> results = new ArrayList<>();
        for (LootEntry entry : entries) {
            if (rng.nextDouble() <= entry.getChance()) {
                int amount = entry.getMinAmount()
                        + rng.nextInt(entry.getMaxAmount() - entry.getMinAmount() + 1);
                ItemStack item = entry.getItem().clone();
                item.setAmount(amount);
                results.add(item);
            }
        }
        return results;
    }

    /** Drop vat pham xung quanh location */
    public void dropAt(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        for (ItemStack item : roll()) {
            world.dropItemNaturally(loc, item);
        }
    }

    // ── Static factory helpers ────────────────────────────────────────────────

    /** Tao LootTable don gian tu config string: "DIAMOND:1-3:0.5,GOLD_INGOT:2-5:0.8" */
    public static LootTable fromString(String config) {
        LootTable table = new LootTable();
        if (config == null || config.isBlank()) return table;

        for (String part : config.split(",")) {
            String[] tokens = part.trim().split(":");
            if (tokens.length < 3) continue;
            try {
                Material mat      = Material.valueOf(tokens[0].toUpperCase());
                String[] range    = tokens[1].split("-");
                int      minAmt   = Integer.parseInt(range[0]);
                int      maxAmt   = range.length > 1 ? Integer.parseInt(range[1]) : minAmt;
                double   chance   = Double.parseDouble(tokens[2]);
                table.addEntry(new LootEntry(new ItemStack(mat), minAmt, maxAmt, chance));
            } catch (Exception ignored) {}
        }
        return table;
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    public static class LootEntry {
        private final ItemStack item;
        private final int       minAmount;
        private final int       maxAmount;
        private final double    chance;     // 0.0 - 1.0

        public LootEntry(ItemStack item, int minAmount, int maxAmount, double chance) {
            this.item      = item;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance    = Math.min(1.0, Math.max(0.0, chance));
        }

        public ItemStack getItem()      { return item; }
        public int       getMinAmount() { return minAmount; }
        public int       getMaxAmount() { return maxAmount; }
        public double    getChance()    { return chance; }
    }
}
