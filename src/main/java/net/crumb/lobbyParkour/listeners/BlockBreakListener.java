package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.utils.MMUtils;
import net.crumb.lobbyParkour.utils.MessageType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.sql.SQLException;
import java.util.List;

public class BlockBreakListener implements Listener {
    private final LobbyParkour plugin;

    public BlockBreakListener(LobbyParkour plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location blockLocation = block.getLocation();
        Location aboveLocation = blockLocation.clone().add(0, 1, 0);

        try {
            Query query = new Query(ParkoursDatabase.getConnection());

            List<Location> checkLocations = List.of(blockLocation, aboveLocation);

            for (Location loc : checkLocations) {
                // Check start location
                String startMap = query.getMapnameByPkSpawn(loc);
                if (startMap != null) {
                    Location startLoc = query.getStartLocation(startMap);
                    if (startLoc != null && startLoc.equals(loc)) {
                        e.setCancelled(true);
                        if (e.getPlayer().hasPermission("lpk.admin")) {
                            MMUtils.sendMessage(e.getPlayer(), "You cannot break this block! If you wish to delete this parkour please use <hover:show_text:'<color:#52a3ff>✎</color> <color:#ffeb7a>Click to use!</color>'><click:run_command:'/lpk'><white>/lpk</white></click></hover>!", MessageType.WARNING);
                        }
                        return;
                    }
                }

                // Check end location
                String endMap = query.getMapNameByPkEnd(loc);
                if (endMap != null) {
                    Location endLoc = query.getEndLocation(endMap);
                    if (endLoc != null && endLoc.equals(loc)) {
                        e.setCancelled(true);
                        if (e.getPlayer().hasPermission("lpk.admin")) {
                            MMUtils.sendMessage(e.getPlayer(), "You cannot break this block! If you wish to delete this parkour please use <hover:show_text:'<color:#52a3ff>✎</color> <color:#ffeb7a>Click to use!</color>'><click:run_command:'/lpk'><white>/lpk</white></click></hover>!", MessageType.WARNING);
                        }
                        return;
                    }
                }

                if (query.isCheckpoint(loc)) {
                    e.setCancelled(true);
                    if (e.getPlayer().hasPermission("lpk.admin")) {
                        MMUtils.sendMessage(e.getPlayer(), "You cannot break this block! If you wish to delete this parkour please use <hover:show_text:'<color:#52a3ff>✎</color> <color:#ffeb7a>Click to use!</color>'><click:run_command:'/lpk'><white>/lpk</white></click></hover>!", MessageType.WARNING);
                    }
                    return;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
