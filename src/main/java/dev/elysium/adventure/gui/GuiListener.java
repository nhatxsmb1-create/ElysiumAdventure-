package dev.elysium.adventure.gui;

import dev.elysium.core.gui.ElysiumGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {

    // Player UUID -> GUI dang mo
    private static final Map<UUID, ElysiumGui> openGuis = new HashMap<>();

    public static void register(UUID uuid, ElysiumGui gui) {
        openGuis.put(uuid, gui);
    }

    public static void unregister(UUID uuid) {
        openGuis.remove(uuid);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        ElysiumGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;
        if (!e.getInventory().equals(gui.getInventory())) return;
        e.setCancelled(true);
        gui.handleClick(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof org.bukkit.entity.Player player)) return;
        ElysiumGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;
        gui.onClose(player);
        openGuis.remove(player.getUniqueId());
    }
}
