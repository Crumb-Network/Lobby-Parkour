package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.guis.*;
import net.crumb.lobbyParkour.systems.*;
import net.crumb.lobbyParkour.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;

public class InventoryClickListener implements Listener {
    private static final LobbyParkour plugin = LobbyParkour.getInstance();
    private static final TextFormatter textFormatter = new TextFormatter();
    private static final Map<UUID, String> newCheckpointsCache = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (!player.hasPermission("lpk.admin")) return;

        if (clickedInventory == null) return;
        if (clickedInventory.getType().equals(InventoryType.ANVIL)) return;


        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        String displayName = "";
        List<String> itemLore = new ArrayList<>();
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta.hasDisplayName()) {
            displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }

        if (meta != null && meta.lore() != null) {
            meta.lore().forEach(component -> itemLore.add(PlainTextComponentSerializer.plainText().serialize(component)));
        }

        String menuTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        menuTitle = menuTitle.trim();

        if (menuTitle.equals("ʟᴏʙʙʏ ᴘᴀʀᴋᴏᴜʀ")) {
            event.setCancelled(true);


            if (displayName.equals("+ Create a new parkour")) {
                player.getInventory().clear(0);
                player.getOpenInventory().close();
                MMUtils.sendMessage(player, "Please place the start of your parkour. <gray>(1/3)</gray>", MessageType.INFO);
                ItemMaker.giveItemToPlayer(player, ItemMaker.createItem("minecraft:light_weighted_pressure_plate", 1, "<green>Parkour Start", Arrays.asList("<gray>Place this where you want", "<gray>your parkour to start.")), 0);
            }   player.getInventory().setHeldItemSlot(0);

            if (displayName.equals("⚑ Parkour List")) {
                MapListMenu.openMenu(player);
            }

            if (displayName.equals("🔁 Reload Parkours")) {
                player.getOpenInventory().close();
                ReloadParkour.reload(player);
            }

            if (displayName.equals("✯ Parkour Leaderboards")) {
                player.getOpenInventory().close();
                LeaderboardMenu.openMenu(player);
            }

        }

        if (menuTitle.equals("Leaderboard List")) {
            event.setCancelled(true);

            if (displayName.equals("Back")) {
                MainMenu.openMenu(player);
            }

            if (displayName.equals("Close")) {
                clickedInventory.close();
            }

            if (displayName.equals("New Leaderboard")) {
                clickedInventory.close();
                ItemStack lbItem = ItemMaker.createItem("minecraft:name_tag", 0, "<green>Place Leaderboard", List.of("<gray>Place this where you want", "<gray>your leaderboard to be"));
                player.getInventory().setItem(0, lbItem);
            }

            if (displayName.contains("(") && displayName.contains(")")) {
                int start = displayName.indexOf("#") + 1;
                int end = displayName.indexOf(")", start);
                int leaderboardId = Integer.parseInt(displayName.substring(start, end));
                Location location = null;

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());

                    location = query.getLeaderboardLocation(leaderboardId);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                if (location == null) {
                    MMUtils.sendMessage(player, "Failed to teleport you to the leaderboard!", MessageType.ERROR);
                    return;
                }

                if (event.isLeftClick()) {
                    SchedulerUtils.teleport(player, location);
                } else {
                    LeaderboardManager leaderboardManager = new LeaderboardManager();
                    leaderboardManager.deleteLeaderboard(leaderboardId);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
                    player.getInventory().close();
                }
            }


        }

        if (menuTitle.equals("Parkour List")) {
            event.setCancelled(true);

            if (displayName.equals("Back")) {
                MainMenu.openMenu(player);
            }

            if (displayName.equals("Close")) {
                clickedInventory.close();
            }

            if (clickedItem.getType().toString().equals("GRASS_BLOCK")) {
                MapManageMenu.openMenu(player, displayName);
            }
        }

        if (menuTitle.equals("Manage Parkour")) {
            event.setCancelled(true);

            if (displayName.equals("Back")) {
                MapListMenu.openMenu(player);
            }

            if (displayName.equals("Close")) {
                clickedInventory.close();
            }

            if (displayName.equals("Rename Parkour")) {
                MapManageMenu.openRenameAnvil(player, itemLore.get(1));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
            }

            if (displayName.equals("Delete Parkour")) {
                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());
                    Component loreLine = event.getView().getItem(10).getItemMeta().lore().get(1);
                    String name = PlainTextComponentSerializer.plainText().serialize(loreLine);

                    // Remove start plate and entity
                    Location startLocation = query.getStartLocation(name);
                    startLocation.getBlock().setType(Material.AIR);

                    UUID startEntityUuid = query.getStartEntityUuid(name);
                    World startLocationWorld = startLocation.getWorld();
                    EntityRemove.suppress(startEntityUuid);
                    Entity startEntity = startLocationWorld.getEntity(startEntityUuid);
                    assert startEntity != null;
                    startEntity.remove();

                    // Remove end plate and entity
                    Location endLocation = query.getEndLocation(name);
                    endLocation.getBlock().setType(Material.AIR);

                    UUID endEntityUuid = query.getEndEntityUuid(name);
                    World endLocationWorld = endLocation.getWorld();
                    EntityRemove.suppress(endEntityUuid);
                    Entity endEntity = endLocationWorld.getEntity(endEntityUuid);
                    assert endEntity != null;
                    endEntity.remove();

                    // Remove checkpoints and entities
                    int parkourId = query.getParkourIdFromName(name);
                    List<Object[]> checkpoints = query.getCheckpoints(parkourId);
                    checkpoints.forEach(checkpoint -> {
                        try {
                            query.deleteCheckpoint(parkourId, (Integer) checkpoint[1]);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        Location cpLocation = LocationHelper.stringToLocation(checkpoint[2].toString());
                        cpLocation.getBlock().setType(Material.AIR);

                        UUID cpEntityUuid = (UUID) checkpoint[4];
                        World cpLocationWorld = cpLocation.getWorld();
                        EntityRemove.suppress(cpEntityUuid);
                        Entity cpEntity = cpLocationWorld.getEntity(cpEntityUuid);
                        assert cpEntity != null;
                        cpEntity.remove();
                    });

                    query.deleteParkour(name);

                    player.getInventory().close();
                    MMUtils.sendMessage(player, "The parkour <white>"+name+"</white> has been deleted!", MessageType.INFO);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                MapListMenu.openMenu(player);
            }

            if (displayName.equals("Change End Type")) {
                Component loreLine = event.getView().getItem(10).getItemMeta().lore().get(1);
                String name = PlainTextComponentSerializer.plainText().serialize(loreLine);
                EditPlateTypeMenu.openMenu(player, name, PlateType.END);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
            }

            if (displayName.equals("Change Start Type")) {
                Component loreLine = event.getView().getItem(10).getItemMeta().lore().get(1);
                String name = PlainTextComponentSerializer.plainText().serialize(loreLine);
                EditPlateTypeMenu.openMenu(player, name, PlateType.START);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
            }

            if (displayName.equals("Manage Checkpoints")) {
                Component loreLine = event.getView().getItem(10).getItemMeta().lore().get(1);
                String name = PlainTextComponentSerializer.plainText().serialize(loreLine);
                CheckpointListMenu.openMenu(player, name);
            }

            if (displayName.equals("Teleport to plate")) {
                Component loreLine = event.getView().getItem(10).getItemMeta().lore().get(1);
                String name = PlainTextComponentSerializer.plainText().serialize(loreLine);

                Location loc = null;

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());

                    loc = query.getStartLocation(name);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                if (loc == null) return;

                SchedulerUtils.teleport(player, loc);
                MMUtils.sendMessage(player, "You have been teleported to the start of <white>"+name+"</white>!", MessageType.INFO);
            }

            if (displayName.equals("Place Leaderboard")) {
                Component loreLine = event.getView().getItem(16).getItemMeta().lore().get(1);
                String name = PlainTextComponentSerializer.plainText().serialize(loreLine);
                player.getInventory().clear(0);
                player.getOpenInventory().close();
                ItemMaker.giveItemToPlayer(player, ItemMaker.createItem("minecraft:book", 1, "<green>Place Leaderboard", Arrays.asList("<gray>Place this where you want", "<gray>your leaderboard to be.", "", "<gray>Parkour:", "<white>" + name)), 0);
                player.getInventory().setHeldItemSlot(0);
                MMUtils.sendMessage(player, "Please place the leaderboard for <white>" + name + "</white> anywhere you want!", MessageType.INFO);
            }
        }

        if (menuTitle.equals("Change Type")) {
            event.setCancelled(true);

            if (displayName.equals("Back")) {
                String currentParkour = PlainTextComponentSerializer.plainText().serialize(clickedInventory.getItem(0).lore().get(0));
                MapManageMenu.openMenu(player, currentParkour);
            }

            if (displayName.equals("Close")) {
                clickedInventory.close();
            }

            final String itemName = displayName;

            PressurePlates.get().forEach(plate -> {
                if (itemName.equals(PressurePlates.formatPlateName(plate))) {
                    Component loreLine = event.getView().getItem(0).getItemMeta().lore().get(0);
                    String menuTypeString = PlainTextComponentSerializer.plainText().serialize(event.getView().getItem(0).getItemMeta().lore().get(1));
                    PlateType menuType = PlateType.valueOf(menuTypeString);
                    Location loc = null;

                    // Get location of plate
                    try {
                        ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                        Query query = new Query(database.getConnection());
                        if (menuType == PlateType.START) {
                            loc = query.getStartLocation(PlainTextComponentSerializer.plainText().serialize(loreLine));
                        } else if (menuType == PlateType.END) {
                            loc = query.getEndLocation(PlainTextComponentSerializer.plainText().serialize(loreLine));
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }

                    if (loc == null) return;
                    Material material = Material.matchMaterial(plate.replace("minecraft:", ""));

                    if (material != null) {
                        loc.getBlock().setType(material);

                        try {
                            ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                            Query query = new Query(database.getConnection());
                            String currentParkour = PlainTextComponentSerializer.plainText().serialize(clickedInventory.getItem(0).lore().get(0));

                            if (menuType == PlateType.START) {
                                query.updateStartType(currentParkour, plate);
                            } else {
                                query.updateEndType(currentParkour, plate);
                            }

                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
                            EditPlateTypeMenu.openMenu(player, currentParkour, menuType);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                    } else {
                        plugin.getLogger().warning("Unknown material: " + plate);
                    }
                }
            });

        }

        if (menuTitle.equals("Manage Checkpoint")) {
            event.setCancelled(true);

            Component loreLine = event.getView().getItem(0).getItemMeta().lore().getFirst();
            Component locationLoreLine = event.getView().getItem(0).getItemMeta().lore().get(1);
            Location location = LocationHelper.stringToLocation(PlainTextComponentSerializer.plainText().serialize(locationLoreLine));
            String parkourName = PlainTextComponentSerializer.plainText().serialize(loreLine);

            if (displayName.equals("Close")) {
                clickedInventory.close();
            }

            if (displayName.equals("Back")) {
                CheckpointListMenu.openMenu(player, parkourName);
            }

            if (displayName.equals("Change Type")) {
                CheckpointPlateType.openMenu(player, parkourName, PlateType.CHECKPOINT, location);
            }

            if (displayName.equals("Relocate Checkpoint")) {
                clickedInventory.close();

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);

                    // Remove the old plate and entity
                    location.getBlock().setType(Material.AIR);
                    UUID entityUuid = query.getCheckpointDisplay(LocationHelper.locationToString(location));
                    World world = location.getWorld();
                    EntityRemove.suppress(entityUuid);
                    Entity entity = world.getEntity(entityUuid);
                    assert entity != null;
                    entity.remove();

                    // Give the player the item
                    String plateType = query.getCheckpointType(LocationHelper.locationToString(location)).getType().getKey().toString();
                    ItemStack checkpointItem = ItemMaker.createItem(plateType, 1, "<blue>Relocate Checkpoint", List.of("<gray>Place this where you want", "<gray>your checkpoint to be."));
                    player.getInventory().setItem(0, checkpointItem);
                    player.getInventory().setHeldItemSlot(0);

                    // Create a new relocation session
                    RelocateCheckpoint session = new RelocateCheckpoint();
                    int parkourId = query.getParkourIdByCheckpointLocation(LocationHelper.locationToString(location));
                    int checkpointIndex = query.getCheckpointIndex(LocationHelper.locationToString(location));
                    session.setCheckpointIndex(checkpointIndex);
                    session.setParkourId(parkourId);
                    RelocateSessionManager.getRelocationSessions().put(player.getUniqueId(), session);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            if (displayName.equals("Delete Checkpoint")) {
                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());

                    int parkourId = query.getParkourIdByCheckpointLocation(LocationHelper.locationToString(location));
                    int checkpointIndex = query.getCheckpointIndex(LocationHelper.locationToString(location));

                    // Remove entity and block position
                    UUID entityUuid = query.getCheckpointDisplay(LocationHelper.locationToString(location));
                    query.removeCheckpoint(parkourId, checkpointIndex);
                    location.getBlock().setType(Material.AIR);
                    World world = location.getWorld();
                    EntityRemove.suppress(entityUuid);
                    Entity entity = world.getEntity(entityUuid);
                    assert entity != null;
                    entity.remove();

                    // Update the existing indexes
                    List<Object[]> oldCheckpoints = query.getCheckpoints(parkourId);
                    oldCheckpoints.forEach(cp -> {
                        int oldIndex = (Integer) cp[1];
                        if (oldIndex >  checkpointIndex) {
                            int newIndex = oldIndex - 1;
                            try {
                                query.updateCheckpointIndex(parkourId, oldIndex, newIndex);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

                    // Update existing holograms
                    List<Object[]> checkpoints = query.getCheckpoints(parkourId);
                    String pkName = query.getParkourNameById(parkourId);
                    checkpoints.forEach(checkpoint -> {
                        UUID entityUUID = (UUID) checkpoint[4];
                        int cpIndex = (Integer) checkpoint[1];
                        Entity displayEntity = world.getEntity(entityUUID);

                        if (!(displayEntity instanceof TextDisplay cpTextDisplay)) return;

                        Map<String, String> cpPlaceholders = Map.of(
                                "checkpoint", String.valueOf(cpIndex),
                                "checkpoint_total", String.valueOf(checkpoints.size()),
                                "parkour_name", pkName
                        );

                        Component cpText = textFormatter.formatString(ConfigManager.getFormat().getCheckpointPlate(), cpPlaceholders);
                        cpTextDisplay.text(cpText);
                    });

                    clickedInventory.close();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (menuTitle.equals("Change Checkpoint Type")) {
            event.setCancelled(true);

            Component loreLine = event.getView().getItem(0).getItemMeta().lore().getFirst();
            Component locationLoreLine = event.getView().getItem(0).getItemMeta().lore().get(1);
            Location location = LocationHelper.stringToLocation(PlainTextComponentSerializer.plainText().serialize(locationLoreLine));
            String parkourName = PlainTextComponentSerializer.plainText().serialize(loreLine);

            if (location == null) {
                MMUtils.sendMessage(player, "Coulnd't find location of the checkpoint.", MessageType.ERROR);
            }

            if (displayName.equals("Close")) {
                clickedInventory.close();
            }

            if (displayName.equals("Back")) {
                CheckpointEditMenu.openMenu(player, parkourName, location);
            }

            final String itemName = displayName;

            PressurePlates.get().forEach(plate -> {
                if (itemName.equals(PressurePlates.formatPlateName(plate))) {
                    Material material = Material.matchMaterial(plate.replace("minecraft:", ""));

                    if (material != null) {
                        location.getBlock().setType(material);

                        try {
                            ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                            Query query = new Query(database.getConnection());
                            query.updateCheckpointType(LocationHelper.locationToString(location), plate);

                            CheckpointPlateType.openMenu(player, parkourName, PlateType.CHECKPOINT, location);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }

                    } else {
                        plugin.getLogger().warning("Unknown material: " + plate);
                    }
                }
            });

        }

        if (menuTitle.equals("Checkpoint List")) {
            event.setCancelled(true);

            if (displayName.equals("Close")) {
                clickedInventory.close();
            }

            if (displayName.equals("Back")) {
                Component loreLine = event.getView().getItem(0).getItemMeta().lore().get(0);
                String parkourName = PlainTextComponentSerializer.plainText().serialize(loreLine);
                MapManageMenu.openMenu(player, parkourName);
            }

            if (displayName.contains("Checkpoint #")) {
                Component loreLine = event.getView().getItem(0).getItemMeta().lore().get(0);
                String parkourName = PlainTextComponentSerializer.plainText().serialize(loreLine);
                int checkpointIndex = Integer.parseInt(displayName.replace("Checkpoint #", ""));
                Location location = null;

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());
                    int parkourId = query.getParkourIdFromName(parkourName);
                    location = query.getCheckpointLocation(parkourId, checkpointIndex);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                if (location == null) {
                    MMUtils.sendMessage(player, "Failed to read the location of the checkpoint.", MessageType.ERROR);
                    return;
                }

                CheckpointEditMenu.openMenu(player, parkourName, location);
            }

            if (displayName.equals("New Checkpoint")) {
                Component loreLine = event.getView().getItem(0).getItemMeta().lore().get(0);
                String parkourName = PlainTextComponentSerializer.plainText().serialize(loreLine);
                clickedInventory.close();

                Map<UUID, String> cache = getNewCheckpointsCache();
                cache.put(player.getUniqueId(), parkourName);

                String actionId = player.getUniqueId() + "cancel-cp-setup";
                ItemStack cancelItem = ActionItemMaker.createItem("minecraft:barrier", 1, "<red>Cancel", List.of("<gray>Cancel the checkpoint setup."), actionId);
                ItemActionHandler.registerAction(actionId, p -> {
                    p.getInventory().clear();
                    getNewCheckpointsCache().remove(player.getUniqueId());
                });

                ItemStack checkpointItem = ItemMaker.createItem("minecraft:heavy_weighted_pressure_plate", 1, "<green>Checkpoint", new ArrayList<>());
                player.getInventory().setItem(0, checkpointItem);
                player.getInventory().setItem(1, cancelItem);
            }
        }
    }

    public static Map<UUID, String> getNewCheckpointsCache() {
        return newCheckpointsCache;
    }
}
