package dev.elysium.adventure.gui;

import dev.elysium.adventure.ElysiumAdventure;
import dev.elysium.adventure.party.Party;
import dev.elysium.core.gui.ElysiumGui;
import dev.elysium.core.gui.GuiButton;
import dev.elysium.core.gui.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyGui extends ElysiumGui {

    private final ElysiumAdventure plugin;

    public PartyGui(ElysiumAdventure plugin) {
        super("&5&lParty", 54);
        this.plugin = plugin;
    }

    @Override
    public void build(Player viewer) {
        fill(ItemBuilder.filler());

        Party party = plugin.getPartyManager().getParty(viewer);
        boolean isLeader = party != null && party.isLeader(viewer.getUniqueId());

        if (party == null) {
            // Khong trong party
            fill(22, new ItemBuilder(Material.BARRIER)
                    .name("&cBan chua trong party nao!")
                    .lore("&7Hay duoc moi hoac moi nguoi khac")
                    .build());
            return;
        }

        // Hien thi thanh vien (slot 10-16, 19-25)
        int[] memberSlots = {10, 11, 12, 13, 14, 15, 16};
        List<UUID> members = new ArrayList<>(party.getMembers());

        for (int i = 0; i < members.size() && i < memberSlots.length; i++) {
            UUID   uuid   = members.get(i);
            Player member = Bukkit.getPlayer(uuid);
            String name   = member != null ? member.getName() : "Offline";
            boolean isThisLeader = party.isLeader(uuid);

            // Skull cua player
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm    = (SkullMeta) skull.getItemMeta();
            if (member != null) sm.setOwningPlayer(member);
            sm.setDisplayName(color(isThisLeader ? "&e&l👑 " + name : "&f" + name));

            List<String> lore = new ArrayList<>();
            lore.add(isThisLeader ? "&eParty Leader" : "&7Thanh vien");
            lore.add("");
            if (isLeader && !uuid.equals(viewer.getUniqueId())) {
                lore.add("&cClick de kick");
            }
            sm.setLore(lore.stream().map(this::color).collect(java.util.stream.Collectors.toList()));
            skull.setItemMeta(sm);

            final UUID targetUuid = uuid;
            setButton(memberSlots[i], new GuiButton(skull, e -> {
                e.setCancelled(true);
                if (!isLeader) return;
                if (targetUuid.equals(viewer.getUniqueId())) return;
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null) {
                    plugin.getPartyManager().kickMember(viewer, target);
                    // Refresh GUI
                    new PartyGui(plugin).open(viewer);
                }
            }));
        }

        // Nut Roi party (slot 45)
        setButton(45, new GuiButton(
                new ItemBuilder(Material.RED_BED).name("&cRoi Party")
                        .lore("&7Click de roi party").build(),
                e -> {
                    e.setCancelled(true);
                    viewer.closeInventory();
                    plugin.getPartyManager().leaveParty(viewer);
                }
        ));

        // Nut Giai tan (slot 53, chi leader)
        if (isLeader) {
            setButton(53, new GuiButton(
                    new ItemBuilder(Material.TNT).name("&4Giai Tan Party")
                            .lore("&7Click de giai tan party").glow().build(),
                    e -> {
                        e.setCancelled(true);
                        viewer.closeInventory();
                        plugin.getPartyManager().disbandParty(party);
                    }
            ));
        }

        // Info party (slot 49)
        fill(49, new ItemBuilder(Material.NETHER_STAR)
                .name("&5Party Info")
                .lore(
                    "&7Thanh vien: &f" + party.getSize() + "/" + plugin.getAdventureConfig().getPartyMaxSize(),
                    "&7Truong party: &e" + (Bukkit.getPlayer(party.getLeader()) != null
                            ? Bukkit.getPlayer(party.getLeader()).getName() : "Unknown")
                ).build());
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
