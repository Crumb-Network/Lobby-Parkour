package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.systems.ParkourSession;
import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import net.crumb.lobbyParkour.utils.MMUtils;
import net.crumb.lobbyParkour.utils.MessageType;
import net.crumb.lobbyParkour.utils.SchedulerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.sql.SQLException;

public class PlayerDeathListener implements Listener {
    private static final LobbyParkour plugin = LobbyParkour.getInstance();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (ParkourSessionManager.isInSession(event.getPlayer().getUniqueId())) {
            Player player = event.getPlayer();
            ParkourSession session = ParkourSessionManager.getSession(player.getUniqueId());
            event.setCancelled(true);

            int lastIndex = ParkourSessionManager.getSessions().get(player.getUniqueId()).getLastReachedCheckpointIndex();
            try {
                Query query = new Query(ParkoursDatabase.getConnection());
                Location loc;
                int parkourId = query.getParkourIdFromName(session.getParkourName());
                if (lastIndex != 0) {
                    loc = query.getCheckpointLocation(parkourId, lastIndex);
                } else {
                    loc = query.getStartLocation(session.getParkourName());
                }

                Location teleportLocation = new Location(loc.getWorld(), loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5, player.getYaw(), player.getPitch());
                SchedulerUtils.teleport(player, teleportLocation);
            } catch (SQLException ex) {
                MMUtils.sendMessage(player, "Could not get checkpoints from database.", MessageType.ERROR);
            }
        }
    }
}
