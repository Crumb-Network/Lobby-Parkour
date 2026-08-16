package net.crumb.lobbyParkour.guis;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.utils.ItemMaker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CheckpointListMenu {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LobbyParkour plugin = LobbyParkour.getInstance();

    public static void openMenu(Player player, String parkourName) {
        if (!player.hasPermission("lpk.admin")) return;
        Inventory gui = Bukkit.createInventory(null, 9 * 6, miniMessage.deserialize("<bold><gradient:#2200cc:#5e43e6>Checkpoint List<reset>"));
        List<String> emptyLore = new ArrayList<>();

        ItemStack background = ItemMaker.createItem("minecraft:blue_stained_glass_pane", 1, "", emptyLore);
        ItemStack backArrow = ItemMaker.createItem("minecraft:arrow", 1, "<green>Back", List.of("<gray>Previous page"));
        ItemStack closeButton = ItemMaker.createItem("minecraft:barrier", 1, "<red>Close", emptyLore);
        ItemStack newCheckpointButton = ItemMaker.createItem("minecraft:heavy_weighted_pressure_plate", 1, "<green>New Checkpoint", List.of("<gray>Setup a new checkpoint", "<yellow>Click to setup!"));

        // Make secret info item
        ItemStack secretItem = new ItemStack(Material.BLUE_STAINED_GLASS_PANE, 1);
        ItemMeta secretMeta = secretItem.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(parkourName));
        secretMeta.lore(lore);
        secretMeta.setHideTooltip(true);

        secretItem.setItemMeta(secretMeta);


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

        gui.setItem(45, newCheckpointButton);
        gui.setItem(48, backArrow);
        gui.setItem(49, closeButton);
        gui.setItem(0, secretItem);

        try {
            Query query = new Query(ParkoursDatabase.getConnection());
            int parkourId = query.getParkourIdFromName(parkourName);
            List<Object[]> checkpoints = query.getCheckpoints(parkourId);

            if (checkpoints.isEmpty()) {
                player.openInventory(gui);
                return;
            }

            int[] contentSlots = {
                    10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43
            };

            int index = 0;
            for (Object[] cp : checkpoints) {
                if (index >= contentSlots.length) break;

                ItemStack mapItem = ItemMaker.createItem("minecraft:heavy_weighted_pressure_plate", 1, "<blue>Checkpoint <gray>#" + String.valueOf((Integer) cp[1]), List.of("<yellow>Click to manage!"));
                gui.setItem(contentSlots[index], mapItem);
                index++;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        player.openInventory(gui);
    }
}
