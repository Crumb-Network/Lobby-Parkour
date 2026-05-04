package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.guis.CheckpointEditMenu;
import net.crumb.lobbyParkour.guis.EditPlateTypeMenu;
import net.crumb.lobbyParkour.guis.MapManageMenu;
import net.crumb.lobbyParkour.models.ParkourItem;
import net.crumb.lobbyParkour.systems.LeaderboardUpdater;
import net.crumb.lobbyParkour.systems.ParkourSession;
import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import net.crumb.lobbyParkour.systems.ParkourTimer;
import net.crumb.lobbyParkour.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.util.RayTraceResult;

import java.sql.SQLException;
import java.util.*;

import static net.crumb.lobbyParkour.utils.PressurePlates.isPressurePlate;

public class PlayerInteractListener implements Listener {
    private static final LobbyParkour plugin = LobbyParkour.getInstance();
    private static final TextFormatter textFormatter = new TextFormatter();
    private static final LeaderboardUpdater updater = LeaderboardUpdater.getInstance();

    private final ConfigManager.Items itemConfig = ConfigManager.getItems();
    private final ParkourItem lastCheckpointProps = new ParkourItem(
            itemConfig.getCheckpointItem(),
            itemConfig.getCheckpointAmount(),
            itemConfig.getCheckpointName()
    );

    private final ParkourItem resetItemProps = new ParkourItem(
            itemConfig.getResetItem(),
            itemConfig.getResetAmount(),
            itemConfig.getResetName()
    );

    private final ParkourItem leaveItemProps = new ParkourItem(
            itemConfig.getLeaveItem(),
            itemConfig.getLeaveAmount(),
            itemConfig.getLeaveName()
    );


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Shift + right-click on pressure plate to open parkour manage menu
        if (player.isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK && player.hasPermission("lpk.admin") && PressurePlates.isPressurePlate(event.getClickedBlock().getType())) {
            Block block = event.getClickedBlock();
            if (block == null) return;
            Location location = block.getLocation();
            if (location == null) return;

            String parkourName;

            try {
                ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                Query query = new Query(database.getConnection());

                List<Object[]> pkStarts = query.getAllParkourStarts();
                boolean isPkStart = pkStarts.stream().anyMatch(entry -> (entry[1]).equals(location));

                List<Object[]> pkEnds = query.getAllParkourEnds();
                boolean isPkEnd = pkEnds.stream().anyMatch(entry -> (entry[1]).equals(location));

                // Check for checkpoints
                boolean isPkCheckpoint = false;

                if (!isPkStart && !isPkEnd) {
                    List<Object[]> allPkCheckpoints = query.getCheckpoints();
                    final Integer[] parkourId = {null};

                    allPkCheckpoints.forEach(checkpoint -> {
                        if (compareLocations(LocationHelper.stringToLocation((String) checkpoint[2]), location)) {
                            parkourId[0] = (Integer) checkpoint[5];
                        }
                    });

                    if (parkourId[0] == null) {
                        return;
                    }

                    List<Object[]> pkCheckpoints = query.getCheckpoints(parkourId[0]);
                    if (pkCheckpoints.isEmpty()) {
                        MMUtils.sendMessage(player, "No checkpoints found for parkour with id " + parkourId[0] + ".", MessageType.ERROR);
                        return;
                    }

                    isPkCheckpoint = query.isCheckpoint(parkourId[0]);
                }

                // Execute actions
                if (isPkStart) {
                    // Open the manage menu of the parkour
                    parkourName = query.getMapnameByPkSpawn(location);
                    if (parkourName == null || parkourName.isEmpty()) return;
                    MapManageMenu.openMenu(player, parkourName);
                } else if (isPkEnd) {
                    // Open the edit menu for the end plate
                    parkourName = query.getMapNameByPkEnd(location);
                    if (parkourName == null) return;
                    EditPlateTypeMenu.openMenu(player, parkourName, PlateType.END);
                    return;
                } else if (isPkCheckpoint) {
                    // Open the checkpoint manage menu
                    int parkourId = query.getParkourIdByCheckpointLocation(LocationHelper.locationToString(location));
                    parkourName = query.getParkourNameById(parkourId);
                    CheckpointEditMenu.openMenu(player, parkourName, location);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && player.isSneaking()) {
            RayTraceResult result = player.rayTraceEntities(5);
            if (result == null) return;

            // FIX: does not work
            Entity hitEntity = result.getHitEntity();
            if (hitEntity instanceof TextDisplay textDisplay) {
                player.sendMessage("You clicked on a TextDisplay!");
                event.setCancelled(true); // Optional: prevent other interaction behavior
            }
        }

        // Player stepped on a pressure plate
        if (event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
            if (block != null && isPressurePlate(block.getType())) {
                Location location = block.getLocation();
                boolean isPkStart = false;
                boolean isPkEnd = false;
                boolean isCheckpoint = false;

                String parkourName = "";

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());

                    List<Object[]> pkStarts = query.getAllParkourStarts();
                    isPkStart = pkStarts.stream().anyMatch(entry -> (entry[1]).equals(location));

                    List<Object[]> pkEnds = query.getAllParkourEnds();
                    isPkEnd = pkEnds.stream().anyMatch(entry -> (entry[1]).equals(location));

                    if (!isPkStart && !isPkEnd) {
                        List<Object[]> pkCheckpoints = query.getCheckpoints(query.getParkourIdByCheckpointLocation(LocationHelper.locationToString(location)));
                        isCheckpoint = pkCheckpoints.stream().anyMatch(entry -> (entry[2]).equals(LocationHelper.locationToString(location)));
                    }

                    if (isPkStart) {
                        parkourName = query.getMapnameByPkSpawn(location);
                    } else if (isPkEnd) {
                        parkourName = query.getMapNameByPkEnd(location);
                    } else if (isCheckpoint) {
                        int parkourId = query.getParkourIdByCheckpointLocation(LocationHelper.locationToString(location));
                        parkourName = query.getParkourNameById(parkourId);
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                if (parkourName == null) return;
                if (parkourName.isEmpty()) return;

                String finalParkourName = parkourName;


                // Check if the player is starting a parkour
                if (isPkStart) {
                    ParkourTimer.start();
                    // If player is already doing parkour, reset the timer to 0s
                    if (ParkourSessionManager.isInSession(player.getUniqueId())) {
                        ParkourSession oldSession = ParkourSessionManager.getSession(player.getUniqueId());
                        restoreInventory(player, oldSession);
                        ParkourSessionManager.endSession(player.getUniqueId());
                    }

                    if (!ParkourSessionManager.isInSession(player.getUniqueId())) {
                        ParkourSessionManager.startSession(player.getUniqueId(), parkourName);
                        ParkourSession session = ParkourSessionManager.getSession(player.getUniqueId());
                        session.resetTime();
                        List<String> emptyLore = new ArrayList<>();

                        try {
                            ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                            Query query = new Query(database.getConnection());
                            int parkourId = query.getParkourIdFromName(parkourName);
                            int checkpointCount = query.getCheckpoints(parkourId).size();
                            session.setMaxCheckpoints(checkpointCount);
                        } catch (SQLException ex) {
                            MMUtils.sendMessage(player, "Could not get checkpoints from database.", MessageType.ERROR);
                            ex.printStackTrace();
                        }

                        // Item action ids for right-click actions
                        String resetPkActionId = player.getUniqueId() + "reset-pk";
                        String leavePkActionId = player.getUniqueId() + "leave-pk";
                        String lastCheckpointActionId = player.getUniqueId() + "last-checkpoint-pk";

                        String timer = ParkourTimer.formatTimer(session.getElapsedSeconds(), ConfigManager.getFormat().getTimer(), player);

                        ItemActionHandler.registerAction(resetPkActionId, p -> {
                            Location tpLoc = new Location(location.getWorld(), location.getX() + 0.5, location.getY(), location.getZ() + 0.5, player.getYaw(), player.getPitch());
                            SchedulerUtils.teleport(p, tpLoc);
                            Component resetMessage = textFormatter.formatString(ConfigManager.getFormat().getResetMessage(), player, Map.of(
                                    "parkour_name", finalParkourName,
                                    "player_name", player.getName(),
                                    "timer", timer
                            ));
                            p.sendMessage(resetMessage);
                        });

                        ItemActionHandler.registerAction(leavePkActionId, p -> {
                            restoreInventory(p, ParkourSessionManager.getSession(player.getUniqueId()));

                            if (ParkourSessionManager.isInSession(player.getUniqueId())) {
                                ParkourSessionManager.endSession(player.getUniqueId());

                                // Send leave/cancel message
                                Component leaveMessage = textFormatter.formatString(ConfigManager.getFormat().getCancelMessage(), player, Map.of(
                                        "parkour_name", finalParkourName,
                                        "player_name", player.getName(),
                                        "timer", timer
                                ));
                                p.sendMessage(leaveMessage);
                            }
                        });

                        ItemActionHandler.registerAction(lastCheckpointActionId, p -> {
                            int lastIndex = ParkourSessionManager.getSessions().get(player.getUniqueId()).getLastReachedCheckpointIndex();
                            try {
                                ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                                Query query = new Query(database.getConnection());
                                Location loc = null;
                                int parkourId = query.getParkourIdFromName(finalParkourName);
                                if (lastIndex != 0) {
                                    loc = query.getCheckpointLocation(parkourId, lastIndex);
                                } else {
                                    loc = query.getStartLocation(finalParkourName);
                                }

                                Location teleportLocation = new Location(loc.getWorld(), loc.getX() + 0.5, loc.getY(), loc.getZ() + 0.5, player.getYaw(), player.getPitch());
                                SchedulerUtils.teleport(player, teleportLocation);
                            } catch (SQLException ex) {
                                MMUtils.sendMessage(player, "Could not get checkpoints from database.", MessageType.ERROR);
                            }
                        });

                        // Create parkour items
                        ItemStack restItem = ActionItemMaker.createItem(resetItemProps.getItem(), resetItemProps.getAmount(), resetItemProps.getName(), emptyLore, resetPkActionId);
                        ItemStack leaveItem = ActionItemMaker.createItem(leaveItemProps.getItem(), leaveItemProps.getAmount(), leaveItemProps.getName(), emptyLore, leavePkActionId);
                        ItemStack lastCpItem = ActionItemMaker.createItem(lastCheckpointProps.getItem(), lastCheckpointProps.getAmount(), lastCheckpointProps.getName(), emptyLore, lastCheckpointActionId);

                        // Save inventory before clearing
                        Map<Integer, ItemStack> saveInventory = new HashMap<>();
                        for (int i = 0; i < 36; i++) {
                            saveInventory.put(i, player.getInventory().getItem(i));
                        }
                        saveInventory.put(40, player.getInventory().getItemInOffHand());
                        saveInventory.put(41, player.getInventory().getHelmet());
                        saveInventory.put(42, player.getInventory().getChestplate());
                        saveInventory.put(43, player.getInventory().getLeggings());
                        saveInventory.put(44, player.getInventory().getBoots());
                        session.setInventory(saveInventory);
                        player.getInventory().clear();

                        // Apply inventory layout
                        ItemMaker.giveItemToPlayer(player, restItem, 4);
                        ItemMaker.giveItemToPlayer(player, leaveItem, 5);
                        ItemMaker.giveItemToPlayer(player, lastCpItem, 3);

                        // Send start message
                        Component startMessage = textFormatter.formatString(ConfigManager.getFormat().getStartMessage(), player, Map.of(
                                "parkour_name", parkourName,
                                "player_name", player.getName()
                        ));
                        player.sendMessage(startMessage);
                    }
                } else if (isPkEnd) {
                    // Player finished the parkour
                    if (ParkourSessionManager.isInSession(player.getUniqueId())) {
                        ParkourSession session = ParkourSessionManager.getSession(player.getUniqueId());
                        if (!session.getParkourName().equals(parkourName)) return;

                        if (session.getMaxCheckpoints() != session.getCompletedCheckpoints()) {
                            MMUtils.sendMessage(player, ConfigManager.getFormat().getCheckpointSkipMessage());
                            return;
                        }

                        float timerMillis = ParkourSessionManager.getSession(player.getUniqueId()).getElapsedSeconds();
                        String timer = ParkourTimer.formatTimer(timerMillis, ConfigManager.getFormat().getTimer(), player);
                        ParkourSessionManager.endSession(player.getUniqueId()); // End session

                        // Restore inventory
                        restoreInventory(player, session);

                        try {
                            ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                            Query query = new Query(database.getConnection());
                            int id = query.getParkourIdFromName(parkourName);
                            query.saveTime(player.getUniqueId(), id, timerMillis);
                            updater.updateTimes(id);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                        // Send end message
                        Component endMessage = textFormatter.formatString(ConfigManager.getFormat().getEndMessage(), player, Map.of(
                                "parkour_name", parkourName,
                                "player_name", player.getName(),
                                "timer", timer
                        ));

                        player.sendMessage(endMessage);
                        SoundUtils.playSoundSequence(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.3f, 0);
                        SoundUtils.playSoundSequence(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.6f, 4);
                        SoundUtils.playSoundSequence(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.9f, 8);
                    }
                } else if (isCheckpoint) {
                    UUID uuid = player.getUniqueId();
                    if (ParkourSessionManager.isInSession(uuid)) {
                        ParkourSession session = ParkourSessionManager.getSession(uuid);

                        // Player stepped on a different checkpoint (not a part of the session's parkour)
                        if (!session.getParkourName().equals(parkourName)) return;

                        // Get the index of the stepped pressure plate
                        try {
                            ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                            Query query = new Query(database.getConnection());

                            int checkpointIndex = query.getCheckpointIndex(LocationHelper.locationToString(location));

                            if (checkpointIndex == -1) {
                                return;
                            }


                            int sessionCheckpointIndex = session.getLastReachedCheckpointIndex();
                            if (checkpointIndex < sessionCheckpointIndex) return;

                            if (checkpointIndex == session.getLastReachedCheckpointIndex()) return;
                            if (checkpointIndex != sessionCheckpointIndex + 1) {
                                MMUtils.sendMessage(player, ConfigManager.getFormat().getCheckpointSkipMessage());
                                return;
                            }

                            session.setLastReachedCheckpointIndex(checkpointIndex);
                            session.setCompletedCheckpoints(checkpointIndex);
                            String timer = ParkourTimer.formatTimer(session.getElapsedSeconds(), ConfigManager.getFormat().getTimer(), player);

                            Component checkpointMessage = textFormatter.formatString(ConfigManager.getFormat().getCheckpointMessage(), player, Map.of(
                                    "parkour_name", parkourName,
                                    "player_name", player.getName(),
                                    "checkpoint", String.valueOf(checkpointIndex),
                                    "timer", timer
                            ));
                            player.sendMessage(checkpointMessage);

                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        // Leaderboard item right-click action
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && player.hasPermission("lpk.admin")
                && player.getInventory().getItemInMainHand().getType() == Material.NAME_TAG
        ) {
            ItemStack stack = player.getInventory().getItemInMainHand();
            ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            Component itemNameComponent = meta.displayName();
            String itemName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(itemNameComponent));
            Location eventLocation = Objects.requireNonNull(event.getClickedBlock()).getLocation();
            Location lbLocation = new Location(eventLocation.getWorld(), eventLocation.getBlockX(), eventLocation.getBlockY() + 1, eventLocation.getBlockZ());

            if (itemName.equals("Place Leaderboard")) {
                player.getInventory().remove(stack);
                AnvilView anvilInventory = MenuType.ANVIL.create(player, "Enter Parkour Name");
                anvilInventory.setItem(0, ItemMaker.createItem("minecraft:paper", 1, "Paper", Collections.emptyList()));
                RenameItemListener.setLbLocation(lbLocation);
                player.openInventory(anvilInventory);
            }
        }
    }

    public static boolean compareLocations(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        return loc1.getWorld().getName().equals(loc2.getWorld().getName()) &&
                loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }

    public static void restoreInventory(Player player, ParkourSession session) {
        if (player == null) return;
        if (session == null || session.getInventory() == null || session.getInventory().isEmpty()) {
            return;
        }

        // Restore inventory
        player.getInventory().clear();
        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, session.getInventory().get(i));
        }
        player.getInventory().setItemInOffHand(session.getInventory().get(40));
        player.getInventory().setHelmet(session.getInventory().get(41));
        player.getInventory().setChestplate(session.getInventory().get(42));
        player.getInventory().setLeggings(session.getInventory().get(43));
        player.getInventory().setBoots(session.getInventory().get(44));
        session.getInventory().clear();
    }
}
