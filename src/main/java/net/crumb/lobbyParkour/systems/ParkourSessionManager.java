package net.crumb.lobbyParkour.systems;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParkourSessionManager {
    private static final Map<UUID, ParkourSession> sessions = new ConcurrentHashMap<>();

    public static void startSession(UUID uuid, String parkourName) {
        sessions.put(uuid, new ParkourSession(parkourName));
    }

    public static void endSession(UUID uuid) {
        sessions.remove(uuid);
    }

    public static boolean isInSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public static ParkourSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public static Map<UUID, ParkourSession> getSessions() {
        return sessions;
    }

    public static void restoreInventory(Player player) {
        ParkourSession session = getSession(player.getUniqueId());
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
