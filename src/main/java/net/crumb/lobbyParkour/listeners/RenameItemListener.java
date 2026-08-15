package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.guis.MapListMenu;
import net.crumb.lobbyParkour.systems.LeaderboardManager;
import net.crumb.lobbyParkour.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RenameItemListener implements Listener {
    private static final LobbyParkour plugin = LobbyParkour.getInstance();
    private static final TextFormatter textFormatter = new TextFormatter();
    private static Location lbLocation;

    List<String> guiNames = List.of(
            "Rename Parkour",
            "Enter Parkour Name"
    );

    public static void setLbLocation(Location lbLocation) {
        RenameItemListener.lbLocation = lbLocation;
    }

    @EventHandler
    public void onItemRename(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getView().getType() != InventoryType.ANVIL) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!player.hasPermission("lpk.admin")) return;

        if (!guiNames.contains(title)) return;

        if (e.getSlot() == 0) {
            e.setCancelled(true);
            return;
        }
        if (e.getSlot() != 2) return;

        AnvilView inventory = (AnvilView) e.getView();
        String itemName = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(inventory.getRenameText()));
        String oldName = inventory.getItem(0).getItemMeta().getDisplayName();

        player.closeInventory();

        if (title.equalsIgnoreCase("Rename Parkour")) {
            try {
                Query query = new Query(ParkoursDatabase.getConnection());

                if (query.parkourExists(itemName)) {
                    MMUtils.sendMessage(player, "A parkour with the same already exists!", MessageType.ERROR);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                    player.closeInventory();
                    return;
                }

                query.renameParkour(oldName, itemName);

                UUID startEntityUuid = query.getStartEntityUuid(itemName);
                Location startLocation = query.getStartLocation(itemName);
                World startLocationWorld = startLocation.getWorld();
                Entity startEntity = startLocationWorld.getEntity(startEntityUuid);
                TextDisplay startTextDisplay = (startEntity instanceof TextDisplay) ? (TextDisplay) startEntity : null;
                assert startTextDisplay != null;
                Map<String, String> placeholders = Map.of(
                        "parkour_name", itemName
                );
                Component startText = textFormatter.formatString(ConfigManager.getFormat().getStartPlate(), placeholders);
                startTextDisplay.text(startText);

                UUID endEntityUuid = query.getEndEntityUuid(itemName);
                Location endLocation = query.getEndLocation(itemName);
                World endLocationWorld = endLocation.getWorld();
                Entity endEntity = endLocationWorld.getEntity(endEntityUuid);
                TextDisplay endTextDisplay = (endEntity instanceof TextDisplay) ? (TextDisplay) endEntity : null;
                assert endTextDisplay != null;
                Map<String, String> endPlaceholders = Map.of(
                        "parkour_name", itemName
                );
                Component endText = textFormatter.formatString(ConfigManager.getFormat().getEndPlate(), endPlaceholders);
                endTextDisplay.text(endText);

                updateCheckpoints(itemName);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);

                MMUtils.sendMessage(player, "The parkour <white>"+oldName+"</white> has been renamed to <white>"+itemName+"</white>!", MessageType.INFO);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            MapListMenu.openMenu(player);
        }
        else if (title.equalsIgnoreCase("Enter Parkour Name")) {
            try {
                Query query = new Query(ParkoursDatabase.getConnection());

                int lbCount = query.leaderboardCount();
                if (lbCount >= 28) {
                    MMUtils.sendMessage(player, "You can't have more than 28 parkours!", MessageType.ERROR);
                    return;
                }

                if (!query.parkourExists(itemName)) {
                    MMUtils.sendMessage(player, "A parkour with that name does not exist", MessageType.ERROR);
                    return;
                }

                if (lbLocation == null) {
                    MMUtils.sendMessage(player, "Couldn't place leaderboard because location was null.", MessageType.ERROR);
                    return;
                }

                LeaderboardManager leaderboardManager = new LeaderboardManager();
                leaderboardManager.spawnLeaderboard(lbLocation, itemName);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
                player.getInventory().setItem(0, null);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (e.getView().getType() != InventoryType.ANVIL) return;

        String title = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (!title.contains("Rename Parkour")) return;

        Inventory anvilInventory = e.getInventory();

        ItemStack result = anvilInventory.getItem(2);
        if (result != null) {
            anvilInventory.setItem(2, null);
        }

        anvilInventory.setItem(0, null);
        anvilInventory.setItem(1, null);

        player.getInventory().remove(result);
    }

    public static void updateCheckpoints(String parkourName) {
        try {
            Query query = new Query(ParkoursDatabase.getConnection());

            int parkourId = query.getParkourIdFromName(parkourName);
            List<Object[]> checkpoints = query.getCheckpoints(parkourId);
            checkpoints.forEach(checkpoint -> {
                int cpIndex = (int) checkpoint[1];
                Location cpLocation = null;

                try {
                    cpLocation = query.getCheckpointLocation(parkourId, cpIndex);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                UUID cpEntityUuid = (UUID) checkpoint[4];

                World world = cpLocation.getWorld();

                if (world == null) return;

                Entity entity = world.getEntity(cpEntityUuid);
                if (!(entity instanceof TextDisplay cpTextDisplay)) return;

                Map<String, String> cpPlaceholders = Map.of(
                        "checkpoint", String.valueOf(cpIndex),
                        "checkpoint_total", String.valueOf(checkpoints.size()),
                        "parkour_name", parkourName
                );

                Component cpText = textFormatter.formatString(ConfigManager.getFormat().getCheckpointPlate(), cpPlaceholders);
                cpTextDisplay.text(cpText);
            });
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}