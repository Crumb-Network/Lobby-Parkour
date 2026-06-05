package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerChangeWorldListener implements Listener {

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (ParkourSessionManager.isInSession(player.getUniqueId())) {
            ParkourSessionManager.restoreInventory(player);
            ParkourSessionManager.endSession(player.getUniqueId());
        }
    }
}
