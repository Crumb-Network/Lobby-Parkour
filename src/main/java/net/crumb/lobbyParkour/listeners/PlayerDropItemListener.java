package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import net.crumb.lobbyParkour.systems.RelocateSessionManager;
import net.crumb.lobbyParkour.utils.MMUtils;
import net.crumb.lobbyParkour.utils.MessageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerDropItemListener implements Listener {
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (ParkourSessionManager.isInSession(player.getUniqueId())) {
            event.setCancelled(true); // Cancel the event
            return;
        }

        if (RelocateSessionManager.isInSession(player.getUniqueId())) {
            event.setCancelled(true);
            MMUtils.sendMessage(player, "You can not drop this item! If you want to delete this checkpoint, place it back down and use the delete option.", MessageType.WARNING);
            return;
        }

        if (InventoryClickListener.getNewCheckpointsCache().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.getInventory().clear();
            MMUtils.sendMessage(player, "Canceled setting up a new parkour!", MessageType.INFO);
            InventoryClickListener.getNewCheckpointsCache().remove(player.getUniqueId());
        }

    }

    // Check if a player is trying to remove an item while being in the parkour
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ParkourSessionManager.isInSession(player.getUniqueId())) return; // Check if player is doing parkour

        event.setCancelled(true);
    }
}
