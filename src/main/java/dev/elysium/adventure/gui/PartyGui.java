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

import java.util.*;
import java.util.stream.Collectors;

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

        if (party == null) {
            buildNoParty(viewer);
        } else {
            buildParty(viewer, party);
        }
    }

    // ── Chua co party ────────────────────────────────────────────────────────

    private void buildNoParty(Player viewer) {
        // Center thong bao
        fill(22, new ItemBuilder(Material.GRAY_DYE)
                .name("&cBan chua trong party nao!")
                .lore("&7Tao party hoac cho nguoi khac moi ban")
                .build());

        // Nut Tao Party (slot 20)
        setButton(20, new GuiButton(
                new ItemBuilder(Material.LIME_DYE)
                        .name("&a&lTao Party Moi")
                        .lore("", "&7Click de tao party moi", "&7Sau do moi nguoi choi cung vao dungeon!")
                        .glow()
                        .build(),
                e -> {
                    e.setCancelled(true);
                    plugin.getPartyManager().createParty(viewer);
                    viewer.sendMessage(color("&aTao party thanh cong! Dung &e/party invite <ten> &ede moi."));
                    // Refresh GUI
                    PartyGui newGui = new PartyGui(plugin);
                    GuiListener.register(viewer.getUniqueId(), newGui);
                    newGui.open(viewer);
                }
        ));

        // Nut Huong dan (slot 24)
        fill(24, new ItemBuilder(Material.BOOK)
                .name("&eHuong Dan")
                .lore(
                    "",
                    "&7/party create &f- Tao party",
                    "&7/party invite <ten> &f- Moi nguoi",
                    "&7/party accept &f- Chap nhan loi moi",
                    "&7/party leave &f- Roi party"
                )
                .build());

        // Nut dong
        setButton(49, new GuiButton(
                new ItemBuilder(Material.BARRIER).name("&cDong").build(),
                e -> { e.setCancelled(true); viewer.closeInventory(); }
        ));
    }

    // ── Da co party ──────────────────────────────────────────────────────────

    private void buildParty(Player viewer, Party party) {
        boolean isLeader = party.isLeader(viewer.getUniqueId());

        // Hien thi thanh vien o hang dau (slot 10-16)
        int[] memberSlots = {10, 11, 12, 13, 14, 15, 16};
        List<UUID> members = new ArrayList<>(party.getMembers());

        for (int i = 0; i < members.size() && i < memberSlots.length; i++) {
            UUID   uuid   = members.get(i);
            Player member = Bukkit.getPlayer(uuid);
            String name   = member != null ? member.getName() : "Offline";
            boolean thisIsLeader = party.isLeader(uuid);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm    = (SkullMeta) skull.getItemMeta();
            if (member != null) sm.setOwningPlayer(member);
            sm.setDisplayName(color(thisIsLeader ? "&e&l👑 " + name : "&f" + name));

            List<String> lore = new ArrayList<>();
            lore.add(color(thisIsLeader ? "&eParty Leader" : "&7Thanh vien"));
            lore.add("");
            if (isLeader && !uuid.equals(viewer.getUniqueId())) {
                lore.add(color("&cClick de kick"));
            }
            sm.setLore(lore);
            skull.setItemMeta(sm);

            final UUID targetUuid = uuid;
            setButton(memberSlots[i], new GuiButton(skull, e -> {
                e.setCancelled(true);
                if (!isLeader || targetUuid.equals(viewer.getUniqueId())) return;
                Player target = Bukkit.getPlayer(targetUuid);
                if (target != null) {
                    plugin.getPartyManager().kickMember(viewer, target);
                    PartyGui newGui = new PartyGui(plugin);
                    GuiListener.register(viewer.getUniqueId(), newGui);
                    newGui.open(viewer);
                }
            }));
        }

        // Slot trong con lai trong hang member
        for (int i = members.size(); i < memberSlots.length; i++) {
            fill(memberSlots[i], new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                    .name("&7[Trong]")
                    .lore("&7Cho thanh vien...")
                    .build());
        }

        // Party info (slot 31)
        String leaderName = Bukkit.getPlayer(party.getLeader()) != null
                ? Bukkit.getPlayer(party.getLeader()).getName() : "Unknown";
        fill(31, new ItemBuilder(Material.NETHER_STAR)
                .name("&5&lParty Info")
                .lore(
                    "",
                    "&7Thanh vien: &f" + party.getSize() + "/" + plugin.getAdventureConfig().getPartyMaxSize(),
                    "&7Leader: &e" + leaderName,
                    ""
                )
                .build());

        // Nut Roi party (slot 45)
        setButton(45, new GuiButton(
                new ItemBuilder(Material.RED_BED)
                        .name("&c&lRoi Party")
                        .lore("", "&7Click de roi party")
                        .build(),
                e -> {
                    e.setCancelled(true);
                    viewer.closeInventory();
                    plugin.getPartyManager().leaveParty(viewer);
                }
        ));

        // Nut Moi nguoi (slot 47, chi leader)
        if (isLeader) {
            fill(47, new ItemBuilder(Material.EMERALD)
                    .name("&a&lMoi Nguoi Choi")
                    .lore("", "&7Dung lenh: &e/party invite <ten>")
                    .build());

            // Nut Giai tan (slot 53)
            setButton(53, new GuiButton(
                    new ItemBuilder(Material.TNT)
                            .name("&4&lGiai Tan Party")
                            .lore("", "&cClick de giai tan party!")
                            .glow()
                            .build(),
                    e -> {
                        e.setCancelled(true);
                        viewer.closeInventory();
                        plugin.getPartyManager().disbandParty(party);
                    }
            ));
        }

        // Nut dong (slot 49)
        setButton(49, new GuiButton(
                new ItemBuilder(Material.BARRIER).name("&cDong").build(),
                e -> { e.setCancelled(true); viewer.closeInventory(); }
        ));
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
