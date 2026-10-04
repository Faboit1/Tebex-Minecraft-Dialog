package io.tebex.plugin.event;

import io.tebex.plugin.gui.ListingGui;
import io.tebex.plugin.gui.TebexGuiItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryClickListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof ListingGui)) {
            return;
        }
        ListingGui listingGui = (ListingGui) inventory.getHolder();

        event.setCancelled(true); // Cancel the default click behavior

        int slot = event.getRawSlot();
        TebexGuiItem guiItem = listingGui.getItemInSlot(slot);
        if (guiItem != null && guiItem.getAction() != null) {
            guiItem.getAction().execute(event);  // Invoke the action
        }
    }

    /**
     * A drag is a separate event from a click, so cancelling clicks alone still lets
     * items be dragged into the shop. The shop inventory is transient and never written
     * back anywhere, so anything left in it is destroyed when the view closes.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof ListingGui)) {
            return;
        }

        int topSize = inventory.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
