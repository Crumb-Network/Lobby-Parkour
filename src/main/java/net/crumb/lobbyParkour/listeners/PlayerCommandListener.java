package net.crumb.lobbyParkour.listeners;

import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import net.crumb.lobbyParkour.utils.ConfigManager;
import net.crumb.lobbyParkour.utils.MMUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public class PlayerCommandListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!ParkourSessionManager.isInSession(player.getUniqueId())) return;
        if (!isBlocked(event.getMessage())) return;

        event.setCancelled(true);

        String message = ConfigManager.getSettings().getPlayerCommandsMessage();
        if (message != null && !message.isBlank()) {
            MMUtils.sendMessage(player, message);
        }
    }

    private boolean isBlocked(String rawMessage) {
        ConfigManager.Settings settings = ConfigManager.getSettings();
        if (settings.isDisallowAllPlayerCommands()) return true;

        List<String> blacklist = settings.getBlockedPlayerCommands();
        if (blacklist.isEmpty()) return false;

        String label = normalize(rawMessage);
        if (label.isEmpty()) return false;

        // "/essentials:home" should match a "home" entry, otherwise the namespaced
        // form of the same command would slip straight through the blacklist.
        String withoutNamespace = label.substring(label.indexOf(':') + 1);

        // "/h" should match a "home" entry when it is a registered alias of it.
        String aliasTarget = resolveAlias(withoutNamespace);

        for (String entry : blacklist) {
            if (entry == null) continue;
            String blocked = normalize(entry);
            if (blocked.isEmpty()) continue;

            if (blocked.equals(label) || blocked.equals(withoutNamespace) || blocked.equals(aliasTarget)) {
                return true;
            }
        }

        return false;
    }

    /** Strips the leading slash and any arguments, leaving a lowercase command label. */
    private String normalize(String input) {
        String label = input.trim();
        if (label.startsWith("/")) {
            label = label.substring(1);
        }

        int space = label.indexOf(' ');
        if (space != -1) {
            label = label.substring(0, space);
        }

        return label.toLowerCase(Locale.ROOT);
    }

    /** Resolves a registered plugin command alias to its real name, or null if it is not one. */
    private String resolveAlias(String label) {
        PluginCommand command = Bukkit.getPluginCommand(label);
        return command != null ? command.getName().toLowerCase(Locale.ROOT) : null;
    }
}
