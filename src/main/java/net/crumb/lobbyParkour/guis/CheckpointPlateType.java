package net.crumb.lobbyParkour.guis;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.utils.ItemMaker;
import net.crumb.lobbyParkour.utils.LocationHelper;
import net.crumb.lobbyParkour.utils.PlateType;
import net.crumb.lobbyParkour.utils.PressurePlates;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CheckpointPlateType {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LobbyParkour plugin = LobbyParkour.getInstance();

    public static void openMenu(Player player, String mapName, PlateType menuType, Location location) {
        if (!player.hasPermission("lpk.admin")) return;
        Inventory gui = Bukkit.createInventory(null, 9 * 5, miniMessage.deserialize("<bold><gradient:#2200cc:#5e43e6>Change Checkpoint Type<reset>"));
        List<String> emptyLore = new ArrayList<>();

        ItemStack background = ItemMaker.createItem("minecraft:blue_stained_glass_pane", 1, "", emptyLore);
        ItemStack backArrow = ItemMaker.createItem("minecraft:arrow", 1, "<green>Back", List.of("<gray>Previous page"));
        ItemStack closeButton = ItemMaker.createItem("minecraft:barrier", 1, "<red>Close", emptyLore);

        // Make secret info item
        ItemStack secretItem = new ItemStack(Material.BLUE_STAINED_GLASS_PANE, 1);
        ItemMeta secretMeta = secretItem.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(mapName));
        lore.add(Component.text(LocationHelper.locationToString(location)));
        secretMeta.lore(lore);
        secretMeta.setHideTooltip(true);

        secretItem.setItemMeta(secretMeta);

        ItemStack currentType = null;

        try {
            Query query = new Query(ParkoursDatabase.getConnection());

            if (menuType == PlateType.CHECKPOINT) {
                currentType = query.getCheckpointType(LocationHelper.locationToString(location));
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        int slotCount = 10;
        for (String plate : PressurePlates.get()) {
            while (isBorderSlot(slotCount)) {
                slotCount++;
                if (slotCount >= gui.getSize()) return;
            }

            ItemStack plateItem = ItemMaker.createItem(
                    plate,
                    1,
                    "<green>" + PressurePlates.formatPlateName(plate),
                    List.of("<yellow>Click to select!")
            );

            if (currentType != null && plateItem.getType().equals(currentType.getType())) {
                ItemMeta meta = plateItem.getItemMeta();
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                plateItem.setItemMeta(meta);

                gui.setItem(slotCount, plateItem);
                slotCount++;
                continue;
            }

            gui.setItem(slotCount, plateItem);
            slotCount++;
        }

        int size = gui.getSize();

        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int col = slot % 9;

            boolean isTopOrBottom = row == 0 || row == 4;
            boolean isLeftOrRight = col == 0 || col == 8;

            if (isTopOrBottom || isLeftOrRight) {
                gui.setItem(slot, background);
            }
        }

        gui.setItem(0, secretItem);
        gui.setItem(39, backArrow);
        gui.setItem(40, closeButton);


        player.openInventory(gui);
    }

    private static boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }
}
