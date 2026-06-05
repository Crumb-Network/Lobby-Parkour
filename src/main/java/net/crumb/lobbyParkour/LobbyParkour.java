package net.crumb.lobbyParkour;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.crumb.lobbyParkour.commands.BaseCommand;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.listeners.*;
import net.crumb.lobbyParkour.systems.LeaderboardUpdater;
import net.crumb.lobbyParkour.systems.ParkourSession;
import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import net.crumb.lobbyParkour.utils.ConfigManager;
import net.crumb.lobbyParkour.utils.ItemActionHandler;
import net.crumb.lobbyParkour.utils.SchedulerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Logger;

public final class LobbyParkour extends JavaPlugin {
    private ParkoursDatabase parkoursDatabase;
    private static LobbyParkour instance;

    public static LobbyParkour getInstance() {
        return instance;
    }

    private void startUpMessage() {
        Logger logger = Logger.getLogger("Lobby-Parkour");
        logger.info("-------------------------------");
        logger.info("        LPK - Lobby Parkour       ");
        logger.info("          Version: " + getPluginMeta().getVersion());
        logger.info("           Author: crumb");
        logger.info("--------------------------------");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryClickListener(), this);
        pm.registerEvents(new BlockPlaceListener(this), this);
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new RenameItemListener(), this);
        pm.registerEvents(new EntityRemove(), this);
        pm.registerEvents(new PlayerInteractListener(), this);
        pm.registerEvents(new PlayerDropItemListener(), this);
        pm.registerEvents(new ItemActionHandler(), this);
        pm.registerEvents(new PlayerFlightListener(), this);
        pm.registerEvents(new PlayerDamageListener(), this);
        pm.registerEvents(new PlayerHungerListener(), this);
        pm.registerEvents(new PlayerDeathListener(), this);
        pm.registerEvents(new PlayerLeaveListener(), this);
        pm.registerEvents(new PlayerChangeWorldListener(), this);
    }

    @Override
    public void onEnable() {
        instance = this;

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new BaseCommand().getBuildCommand());
        });

        saveDefaultConfig();
        ConfigManager.loadConfig(getConfig());
        startUpMessage();
        registerListeners();

        try {
            parkoursDatabase = new ParkoursDatabase(getDataFolder().getAbsolutePath() + "/lobby_parkour.db");
        } catch (SQLException ex) {
            ex.printStackTrace();
            getLogger().severe("Failed to connect to the database! " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().warning("Could not find PlaceholderAPI!");
        }

        // Delay the updater init to ensure everything is ready
        SchedulerUtils.runTask(this, () -> {
            LeaderboardUpdater updater = LeaderboardUpdater.getInstance();
            updater.updateCache();
            updater.updateFormat();
            updater.startSpinning();
            updater.updateStatic();
        });
    }


    @Override
    public void onDisable() {
        for (Player player : this.getServer().getOnlinePlayers()) {
            if (ParkourSessionManager.isInSession(player.getUniqueId())) {
                ParkourSessionManager.restoreInventory(player);
                ParkourSessionManager.endSession(player.getUniqueId());
            }
        }

        if (parkoursDatabase == null) {
            return;
        }

        try {
            parkoursDatabase.closeConnection();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
