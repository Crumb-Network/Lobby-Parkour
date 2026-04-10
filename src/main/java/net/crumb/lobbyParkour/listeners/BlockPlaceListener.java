package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.systems.RelocateCheckpoint;
import net.crumb.lobbyParkour.systems.RelocateSessionManager;
import net.crumb.lobbyParkour.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BlockPlaceListener implements Listener {
    private final LobbyParkour plugin;

    public BlockPlaceListener(LobbyParkour plugin) {
        this.plugin = plugin;
    }
    private static final TextFormatter textFormatter = new TextFormatter();

    private final Map<UUID, Map<String, Object>> parkourCache = new HashMap<>();


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("lbk.admin")) return;

        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();

        if (!meta.hasDisplayName()) return;

        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        Location location = event.getBlock().getLocation();

        switch (itemName) {
            case "Parkour Start" -> {
                player.getInventory().remove(item);

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());

                    if (query.parkourMaps().size() == 28) {
                        MMUtils.sendMessage(player, "You can't have more than 28 parkours!", MessageType.ERROR);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                        event.setCancelled(true);
                        return;
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.uu:HH:mm:ss"));
                String parkourName = "New Parkour "+dateTime;

                Map<String, String> placeholders = Map.of(
                        "parkour_name", parkourName
                );
                Component startText = textFormatter.formatString(ConfigManager.getFormat().getStartPlate(), placeholders);

                World world = player.getWorld();
                Location textDisplayLocation = new Location(world, location.getX() + 0.5, location.getY() + 1.0, location.getZ() + 0.5);

                world.getNearbyEntities(textDisplayLocation, 0.5, 0.5, 0.5).stream()
                        .filter(e -> e instanceof TextDisplay)
                        .forEach(Entity::remove);

                TextDisplay display = world.spawn(textDisplayLocation, TextDisplay.class, entity -> {
                    entity.text(startText);
                    entity.setBillboard(Display.Billboard.CENTER);
                });

                UUID entityUuid = display.getUniqueId();
                UUID playerUuid = player.getUniqueId();

                Map<String, Object> parkourData = new HashMap<>();
                parkourData.put("mapName", parkourName);
                parkourData.put("startLocation", location);
                parkourData.put("startEntityUuid", entityUuid);

                parkourCache.put(playerUuid, parkourData);

                ItemMaker.giveItemToPlayer(player, ItemMaker.createItem("minecraft:light_weighted_pressure_plate", 1, "<green>Parkour End", Arrays.asList("<gray>Place this where you want", "<gray>your parkour to end.")), 0);
                MMUtils.sendMessage(player, "Please place the end of your parkour. <gray>(2/3)</gray>", MessageType.INFO);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);

                /*
                MMUtils.sendMessage(player, "<hover:show_text:'<color:#52a3ff>✎</color> <color:#ffeb7a>Click to edit!</color>'><click:run_command:'/lpk'>A new parkour has been initialized. Do <white>/lpk</white> to edit your parkour. <gray>(You can also click this message.)</gray></click></hover>", MessageType.INFO);
                player.getInventory().remove(item);

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());


                    if (query.parkourMaps().size() == 28) {
                        MMUtils.sendMessage(player, "You can't have more than 28 parkours!", MessageType.ERROR);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                        event.setCancelled(true);
                        return;
                    }



                    World world = player.getWorld();
                    Location textDisplayLocation = new Location(world, location.getX() + 0.5, location.getY() + 1.0, location.getZ() + 0.5);
                    String parkourName = "New Parkour "+dateTime;
                    TextDisplay display = world.spawn(textDisplayLocation, TextDisplay.class, entity -> {
                        entity.text(MiniMessage.miniMessage().deserialize("<green>⚑</green> <white>"+parkourName));
                        entity.setBillboard(Display.Billboard.CENTER);
                    });

                    query.createParkour(parkourName, location, display.getUniqueId());
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                 */
            }
            case "Parkour End" -> {
                player.getInventory().remove(item);

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());

                    if (query.parkourMaps().size() == 28) {
                        MMUtils.sendMessage(player, "You can't have more than 28 parkours!", MessageType.ERROR);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                        event.setCancelled(true);
                        return;
                    }

                    Map<String, Object> data = parkourCache.get(player.getUniqueId());

                    if (data == null) {
                        MMUtils.sendMessage(player, "You haven't placed the start plate!", MessageType.ERROR);
                        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                        event.setCancelled(true);
                        return;
                    }

                    String parkourName = (String) data.get("mapName");
                    Location startLocation = (Location) data.get("startLocation");
                    UUID startEntityUuid = (UUID) data.get("startEntityUuid");
                    data.put("endLocation", location);

                    Map<String, String> placeholders = Map.of(
                            "parkour_name", parkourName
                    );
                    Component endText = textFormatter.formatString(ConfigManager.getFormat().getEndPlate(), placeholders);

                    World world = player.getWorld();
                    Location textDisplayLocation = new Location(world, location.getX() + 0.5, location.getY() + 1.0, location.getZ() + 0.5);

                    world.getNearbyEntities(textDisplayLocation, 0.5, 0.5, 0.5).stream()
                            .filter(e -> e instanceof TextDisplay)
                            .forEach(Entity::remove);

                    TextDisplay display = world.spawn(textDisplayLocation, TextDisplay.class, entity -> {
                        entity.text(endText);
                        entity.setBillboard(Display.Billboard.CENTER);
                    });

                    UUID endEntityUuid = display.getUniqueId();

                    query.createParkour(parkourName, startLocation, startEntityUuid, location, endEntityUuid);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
                    MMUtils.sendMessage(player, "<hover:show_text:'<color:#52a3ff>✎</color> <color:#ffeb7a>Click to edit!</color>'><click:run_command:'/lpk'>A new parkour has been initialized. Do <white>/lpk</white> to edit your parkour. <gray>(You can also click this message.)</gray></click></hover>", MessageType.INFO);
                    MMUtils.sendMessage(player, "You can now start placing checkpoints! <gray>(3/3)", MessageType.INFO);

                    player.getInventory().clear();
                    String actionId = player.getUniqueId() + "cancel-cp-setup";
                    ItemStack cancelItem = ActionItemMaker.createItem("minecraft:barrier", 1, "<red>Cancel", List.of("<gray>Cancel the checkpoint setup."), actionId);
                    ItemActionHandler.registerAction(actionId, p -> {
                        p.getInventory().clear();

                    });

                    ItemStack checkpointItem = ItemMaker.createItem("minecraft:heavy_weighted_pressure_plate", 1, "<green>Checkpoint", new ArrayList<>());
                    player.getInventory().setItem(0, checkpointItem);
                    player.getInventory().setItem(1, cancelItem);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }


            }
            case "Checkpoint" -> {
                Map<UUID, String> addCheckpointsCache = InventoryClickListener.getNewCheckpointsCache();
                if (addCheckpointsCache.containsKey(player.getUniqueId())) {
                    String parkourName = addCheckpointsCache.get(player.getUniqueId());
                    createNewCheckpoint(plugin, parkourName, player, location);
                    return;
                }

                Map<String, Object> data = parkourCache.get(player.getUniqueId());
                Location endLocation = (Location) data.get("endLocation");
                if (endLocation == null) {
                    MMUtils.sendMessage(player, "You haven't placed the end or start plate!", MessageType.ERROR);
                    event.setCancelled(true);
                    return;
                }

                String parkourName = (String) data.get("mapName");
                createNewCheckpoint(plugin, parkourName, player, location);
            }

            case "Relocate Checkpoint" -> {
                if (!RelocateSessionManager.isInSession(player.getUniqueId())) {
                    event.setCancelled(true);
                    MMUtils.sendMessage(player, "You are not in a Relocation-Sessions!", MessageType.ERROR);
                    return;
                }

                RelocateCheckpoint session = RelocateSessionManager.getRelocationSessions().get(player.getUniqueId());
                int parkourId = session.getParkourId();
                int cpIndex = session.getCheckpointIndex();

                try {
                    ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
                    Query query = new Query(database.getConnection());
                    // Update location
                    query.updateCheckpointLocation(LocationHelper.locationToString(location), parkourId, cpIndex);

                    String parkourName = query.getParkourNameById(parkourId);

                    // Spawn new display entity
                    List<Object[]> checkpoints = query.getCheckpoints(query.getParkourIdFromName(parkourName));
                    Map<String, String> placeholders = Map.of(
                            "parkour_name", parkourName,
                            "checkpoint", String.valueOf(cpIndex),
                            "checkpoint_total", String.valueOf(checkpoints.size())
                    );

                    Component checkpointText = textFormatter.formatString(ConfigManager.getFormat().getCheckpointPlate(), placeholders);
                    World world = player.getWorld();
                    Location textDisplayLocation = new Location(world, location.getX() + 0.5, location.getY() + 1.0, location.getZ() + 0.5);

                    world.getNearbyEntities(textDisplayLocation, 0.5, 0.5, 0.5).stream()
                            .filter(e -> e instanceof TextDisplay)
                            .forEach(Entity::remove);

                    TextDisplay display = world.spawn(textDisplayLocation, TextDisplay.class, entity -> {
                        entity.text(checkpointText);
                        entity.setBillboard(Display.Billboard.CENTER);
                    });

                    UUID checkpointEntityUuid = display.getUniqueId();
                    query.updateCheckPointEntityUUID(LocationHelper.locationToString(location), checkpointEntityUuid);
                    RelocateSessionManager.removeRelocationSession(player.getUniqueId());
                    player.getInventory().clear();
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void createNewCheckpoint(LobbyParkour plugin, String parkourName, Player player, Location location) {
        try {
            ParkoursDatabase database = new ParkoursDatabase(plugin.getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
            Query query = new Query(database.getConnection());

            int parkourId = query.getParkourIdFromName(parkourName);
            int cpIndex = query.getMaxCheckpointIndex(parkourId) + 1; // New index
            if (cpIndex > 28) {
                MMUtils.sendMessage(player, "You can't have more than 28 parkours!", MessageType.ERROR);
            }

            List<Object[]> checkpoints = query.getCheckpoints(query.getParkourIdFromName(parkourName));

            Map<String, String> placeholders = Map.of(
                    "parkour_name", parkourName,
                    "checkpoint", String.valueOf(cpIndex),
                    "checkpoint_total", String.valueOf(checkpoints.size() + 1)
            );

            Component checkpointText = textFormatter.formatString(ConfigManager.getFormat().getCheckpointPlate(), placeholders);
            World world = player.getWorld();
            Location textDisplayLocation = new Location(world, location.getX() + 0.5, location.getY() + 1.0, location.getZ() + 0.5);

            world.getNearbyEntities(textDisplayLocation, 0.5, 0.5, 0.5).stream()
                    .filter(e -> e instanceof TextDisplay)
                    .forEach(Entity::remove);

            TextDisplay display = world.spawn(textDisplayLocation, TextDisplay.class, entity -> {
                entity.text(checkpointText);
                entity.setBillboard(Display.Billboard.CENTER);
            });

            UUID checkpointEntityUuid = display.getUniqueId();

            query.createCheckpoint(parkourId, cpIndex, location, "minecraft:heavy_weighted_pressure_plate", checkpointEntityUuid);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.1f, 2.0f);

            // Update existing checkpoint displays
            RenameItemListener.updateCheckpoints(parkourName);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
