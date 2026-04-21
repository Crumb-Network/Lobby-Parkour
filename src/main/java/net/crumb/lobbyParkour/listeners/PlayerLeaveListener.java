package net.crumb.lobbyParkour.listeners;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import net.crumb.lobbyParkour.systems.ParkourSession;
import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLeaveListener implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        handlePlayerDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerKickEvent event) {
        handlePlayerDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        handlePlayerDisconnect(event.getPlayer());
    }

    private void handlePlayerDisconnect(Player player) {
        // Remove player from pk session if he was playing
        if (ParkourSessionManager.isInSession(player.getUniqueId())) {
            ParkourSession oldSession = ParkourSessionManager.getSession(player.getUniqueId());
            PlayerInteractListener.restoreInventory(player, oldSession);
            ParkourSessionManager.endSession(player.getUniqueId());
        }
    }
}
