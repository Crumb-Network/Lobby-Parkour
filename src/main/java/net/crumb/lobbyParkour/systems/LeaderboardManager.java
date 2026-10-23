package net.crumb.lobbyParkour.systems;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.listeners.EntityRemove;
import net.crumb.lobbyParkour.utils.ConfigManager;
import net.crumb.lobbyParkour.utils.SchedulerUtils;
import net.crumb.lobbyParkour.utils.TextFormatter;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LeaderboardManager {


    private static final TextFormatter textFormatter = new TextFormatter();
    private static final LobbyParkour plugin = LobbyParkour.getInstance();
    private static final LeaderboardUpdater updater = LeaderboardUpdater.getInstance();

    public void spawnLeaderboard(Location location, String parkourName) {
        try {
            Query query = new Query(ParkoursDatabase.getConnection());

            int parkourId = query.getParkourIdFromName(parkourName);
            int leaderboardId = query.createLeaderboard(parkourId);

            location = location.toCenterLocation();
            location = location.setRotation(0,0);

            var leaderboardFormat = ConfigManager.getFormat().getLeaderboard();
            var displayItemConfig = leaderboardFormat.getDisplayItem();

            Map<String, String> placeholders = Map.of("parkour_name", parkourName);
            Component title = textFormatter.formatString(leaderboardFormat.getTitle(), placeholders);
            Component emptyLine = textFormatter.formatString(leaderboardFormat.getEmptyLineStyle());

            int maxLines = leaderboardFormat.getMaximumDisplayed();

            for (int i = 0; i < maxLines; i++) {
                TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, textDisplay -> {
                    textDisplay.text(emptyLine);
                    textDisplay.setBillboard(Display.Billboard.CENTER);
                });
                query.createLeaderboardLine(leaderboardId, location, display.getUniqueId(), maxLines - i);
                location.add(0.0, 0.4, 0.0);
            }

            TextDisplay titleDisplay = location.getWorld().spawn(location, TextDisplay.class, textDisplay -> {
                textDisplay.text(title);
                textDisplay.setBillboard(Display.Billboard.CENTER);
            });
            query.createLeaderboardLine(leaderboardId, location, titleDisplay.getUniqueId(), 0);

            if (displayItemConfig.isEnabled()) {
                Material displayMaterial = displayItemConfig.getItem();
                boolean enchantGlint = displayItemConfig.hasEnchantGlint();

                ItemStack item = new ItemStack(displayMaterial);
                ItemMeta meta = item.getItemMeta();
                if (enchantGlint) {
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                item.setItemMeta(meta);

                ItemDisplay itemDisplay = location.getWorld().spawn(location.add(0.0, 0.75, 0.0), ItemDisplay.class);
                itemDisplay.setItemStack(item);
                itemDisplay.setBillboard(Display.Billboard.VERTICAL);
                itemDisplay.setTransformation(new Transformation(
                        new Vector3f(0.0f, 0.0f, 0.0f),
                        new Quaternionf(),
                        new Vector3f(0.5f, 0.5f, 0.5f),
                        new Quaternionf()
                ));
                query.createLeaderboardLine(leaderboardId, location, itemDisplay.getUniqueId(), -1);
            }

            updater.updateCache();
            updater.updateTimes(parkourId);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteLeaderboard(int leaderboardId) {
        try {
            Query query = new Query(ParkoursDatabase.getConnection());

            // Get all lines (entity uuid + location) associated with the leaderboard
            for (Object[] line : query.getLeaderboardLineEntities(leaderboardId)) {
                removeLineEntity((UUID) line[0], (Location) line[1]);
            }

            query.deleteLeaderboardLines(leaderboardId);
            query.deleteLeaderboard(leaderboardId);

            updater.updateCache();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Removes every leaderboard belonging to a parkour, both its display entities
     * and its database rows. Call this before deleting the parkour itself.
     */
    public void deleteLeaderboardsForParkour(int parkourId) {
        try {
            Query query = new Query(ParkoursDatabase.getConnection());

            for (int leaderboardId : query.getLeaderboardIdsByParkourId(parkourId)) {
                deleteLeaderboard(leaderboardId);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void removeLineEntity(UUID uuid, Location location) {
        if (location == null || location.getWorld() == null) {
            // No location to work from, best effort on whatever is currently tracked.
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead()) {
                EntityRemove.suppress(uuid);
                SchedulerUtils.runTask(plugin, entity, entity::remove);
            }
            return;
        }

        // The display may sit in an unloaded chunk, in which case it is not tracked
        // yet and would come back as an orphan once the chunk loads again.
        location.getWorld().getChunkAtAsync(location).thenRun(() ->
                SchedulerUtils.runTask(plugin, location, () -> {
                    Entity entity = Bukkit.getEntity(uuid);
                    if (entity == null) {
                        entity = location.getWorld().getEntity(uuid);
                    }
                    if (entity != null && !entity.isDead()) {
                        EntityRemove.suppress(uuid);
                        entity.remove();
                    }
                }));
    }

}
