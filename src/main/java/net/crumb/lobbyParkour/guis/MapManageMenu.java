package net.crumb.lobbyParkour.guis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.utils.ItemMaker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

public class MapManageMenu {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LobbyParkour plugin = LobbyParkour.getInstance();

    public static void openMenu(Player player, String parkourName) {
        if (!player.hasPermission("lpk.admin")) {
            return;
        }
        Inventory gui = Bukkit.createInventory(null, 27, miniMessage.deserialize( "<bold><gradient:#369e36:#2bbf11>Manage Parkour<reset>"));
        ArrayList<String> emptyLore = new ArrayList<String>();
        ItemStack background = ItemMaker.createItem("minecraft:lime_stained_glass_pane", 1, "", emptyLore);
        ItemStack backArrow = ItemMaker.createItem("minecraft:arrow", 1, "<green>Back", List.of("<gray>Previous page"));
        ItemStack closeButton = ItemMaker.createItem("minecraft:barrier", 1, "<red>Close", emptyLore);
        ItemStack deleteButton = ItemMaker.createItem("minecraft:tnt", 1, "<red>Delete Parkour", List.of("<yellow><bold>WARNING! <reset><!italic><yellow>Action can not be undone!", "<yellow>Click to delete!"));
        ItemStack renameButton = ItemMaker.createItem("minecraft:paper", 1, "<green>Rename Parkour", List.of("<gray>Current name:", "<white>" + parkourName, "<yellow>Click to rename!"));

        ItemStack changeStartTypeButton = ItemMaker.createItem("minecraft:light_weighted_pressure_plate", 1, "<green>Change Start Type", List.of("<yellow>Click to change!"));
        ItemStack changeEndTypeButton = ItemMaker.createItem("minecraft:light_weighted_pressure_plate", 1, "<green>Change End Type", List.of("<yellow>Click to change!"));
        ItemStack manageCheckpoints = ItemMaker.createItem("minecraft:heavy_weighted_pressure_plate", 1, "<green>Manage Checkpoints", List.of("<yellow>Click to change!"));
        ItemStack teleportItem = makeTeleportItem(parkourName);

        int size = gui.getSize();
        for (int slot = 0; slot < size; ++slot) {
            boolean isLeftOrRight;
            int row = slot / 9;
            int col = slot % 9;
            boolean isTopOrBottom = row == 0 || row == 2;
            boolean bl = isLeftOrRight = col == 0 || col == 8;
            if (!isTopOrBottom && !isLeftOrRight) continue;
            gui.setItem(slot, background);
        }
        gui.setItem(10, renameButton);

        gui.setItem(11, changeStartTypeButton);
        gui.setItem(12, changeEndTypeButton);
        gui.setItem(13, manageCheckpoints);
        gui.setItem(16, teleportItem);

        gui.setItem(21, backArrow);
        gui.setItem(22, closeButton);
        gui.setItem(26, deleteButton);
        player.openInventory(gui);
    }

    public static void openRenameAnvil(Player player, String parkourName) {
        AnvilView anvilInventory = MenuType.ANVIL.create(player, "Rename Parkour");
        ArrayList<String> emptyLore = new ArrayList<>();
        anvilInventory.setItem(0, ItemMaker.createItem("minecraft:paper", 1, parkourName, emptyLore));
        player.openInventory(anvilInventory);
    }

    public static ItemStack makeTeleportItem(String parkourName) {
        ItemStack item = ItemMaker.createItem("minecraft:ender_pearl", 1, "<green>Teleport to plate", new ArrayList<String>());
        String locationText = "";
        try {
            Query query = new Query(ParkoursDatabase.getConnection());
            Location location = query.getStartLocation(parkourName);
            locationText = String.format("%.2f, %.2f, %.2f", location.getX(), location.getY(), location.getZ());
            locationText = "<!italic><white>" + locationText;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        if (locationText.isEmpty()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        meta.lore(List.of(miniMessage.deserialize(locationText), miniMessage.deserialize("<!italic><yellow>Click to teleport!")));
        item.setItemMeta(meta);
        return item;
    }
}

