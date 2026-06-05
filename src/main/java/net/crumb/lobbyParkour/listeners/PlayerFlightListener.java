package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import net.crumb.lobbyParkour.utils.ConfigManager;
import net.crumb.lobbyParkour.utils.MMUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public class PlayerFlightListener implements Listener {

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (ConfigManager.getSettings().isAllowFly()) return;
        if (ParkourSessionManager.isInSession(player.getUniqueId())) {
            ParkourSessionManager.restoreInventory(player);
            ParkourSessionManager.endSession(player.getUniqueId());
            MMUtils.sendMessage(player, ConfigManager.getFormat().getFlightCancelMessage());
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.2f);
        }
    }
}
