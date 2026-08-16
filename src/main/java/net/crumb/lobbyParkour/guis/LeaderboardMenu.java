package net.crumb.lobbyParkour.guis;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.utils.ItemMaker;
import net.crumb.lobbyParkour.utils.LocationHelper;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaderboardMenu {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LobbyParkour plugin = LobbyParkour.getInstance();

    public static void openMenu(Player player) {
        if (!player.hasPermission("lpk.admin")) return;
        Inventory gui = Bukkit.createInventory(null, 9 * 6, miniMessage.deserialize("<bold><gradient:#369e36:#2bbf11>Leaderboard List<reset>"));
        List<String> emptyLore = new ArrayList<>();

        ItemStack background = ItemMaker.createItem("minecraft:lime_stained_glass_pane", 1, "", emptyLore);
        ItemStack backArrow = ItemMaker.createItem("minecraft:arrow", 1, "<green>Back", List.of("<gray>Previous page"));
        ItemStack closeButton = ItemMaker.createItem("minecraft:barrier", 1, "<red>Close", emptyLore);
        ItemStack newLeaderboard = ItemMaker.createItem("minecraft:name_tag", 1, "<green>New Leaderboard", List.of("<yellow>Click to setup!"));

        int size = gui.getSize();

        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;

            boolean isTopOrBottom = row == 0 || row == 5;
            boolean isLeftOrRight = col == 0 || col == 8;

            if (isTopOrBottom || isLeftOrRight) {
                gui.setItem(slot, background);
            }
        }

        gui.setItem(45, newLeaderboard);
        gui.setItem(48, backArrow);
        gui.setItem(49, closeButton);

        try {
            Query query = new Query(ParkoursDatabase.getConnection());
            Map<Integer, String> leaderboardNames = query.getLeaderboardNames();
            List<Location> leaderboardLocations = query.getLeaderboardLocations();

            int[] contentSlots = {
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            };

            int index = 0;
            for (Map.Entry<Integer, String> entry : leaderboardNames.entrySet()) {
                if (index >= contentSlots.length) break;

                Location loc = leaderboardLocations.get(index);
                String locString = loc != null ? "x=" + loc.getBlockX() + ", z=" + loc.getBlockZ() : "unknown";

                ItemStack mapItem = ItemMaker.createItem(
                        "minecraft:name_tag",
                        1,
                        "<green>" + entry.getValue(),
                        List.of("<dark_gray>Location: " + locString, "<yellow>Left-Click to teleport!", "<yellow>Right-Click to delete!")
                );

                gui.setItem(contentSlots[index], mapItem);
                index++;
            }


        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        player.openInventory(gui);
    }
}
