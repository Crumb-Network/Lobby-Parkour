package net.crumb.lobbyParkour.systems;

import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.database.ParkoursDatabase;
import net.crumb.lobbyParkour.database.Query;
import net.crumb.lobbyParkour.utils.SchedulerUtils;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlaceholderCache {
    private static final LobbyParkour plugin = LobbyParkour.getInstance();

    // Keyed by the lower-cased parkour name
    private static volatile Map<String, ParkourStats> stats = Map.of();
    private static final AtomicBoolean refreshing = new AtomicBoolean(false);
    private static final AtomicBoolean refreshQueued = new AtomicBoolean(false);
    private static SchedulerUtils.Task periodicTask;

    public record Entry(UUID uuid, String playerName, float time) {}

    public record ParkourStats(String name, List<Entry> ranking, Map<UUID, Integer> runsPerPlayer, int totalRuns) {
        public Entry getEntry(int position) {
            if (position < 1 || position > ranking.size()) return null;
            return ranking.get(position - 1);
        }

        public int getPosition(UUID uuid) {
            for (int i = 0; i < ranking.size(); i++) {
                if (ranking.get(i).uuid().equals(uuid)) return i + 1;
            }
            return -1;
        }
    }

    /**
     * Looks up a parkour case-insensitively. Underscores may be used in place of spaces,
     * since many plugins don't handle spaces inside placeholders well.
     */
    public static ParkourStats get(String parkourName) {
        Map<String, ParkourStats> current = stats;
        String key = parkourName.toLowerCase(Locale.ROOT);
        ParkourStats exact = current.get(key);
        if (exact != null) return exact;

        for (Map.Entry<String, ParkourStats> entry : current.entrySet()) {
            if (entry.getKey().replace(' ', '_').equals(key)) return entry.getValue();
        }
        return null;
    }

    public static Collection<ParkourStats> getAll() {
        return stats.values();
    }

    /**
     * Rebuilds the cache asynchronously. Calls made while a refresh is running are coalesced
     * into a single follow-up refresh.
     */
    public static void refresh() {
        if (!refreshing.compareAndSet(false, true)) {
            refreshQueued.set(true);
            return;
        }
        SchedulerUtils.runTaskAsync(plugin, () -> {
            try {
                do {
                    refreshQueued.set(false);
                    load();
                } while (refreshQueued.get());
            } finally {
                refreshing.set(false);
            }
            // A refresh may have been requested between the loop check and releasing the flag
            if (refreshQueued.get()) refresh();
        });
    }

    public static void startPeriodicRefresh(long periodTicks) {
        stopPeriodicRefresh();
        periodicTask = SchedulerUtils.runTaskTimer(plugin, task -> refresh(), periodTicks, periodTicks);
    }

    public static void stopPeriodicRefresh() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    private static void load() {
        try {
            Query query = new Query(ParkoursDatabase.getConnection());
            Map<String, ParkourStats> newStats = new HashMap<>();
            Map<UUID, String> nameCache = new HashMap<>();

            for (String parkourName : query.parkourMaps()) {
                Integer parkourId = query.getParkourIdFromName(parkourName);
                if (parkourId == null) continue;

                List<Map.Entry<UUID, Float>> times = query.getParkourTimes(parkourId);
                times.sort(Comparator.comparingDouble(Map.Entry::getValue));

                Map<UUID, Integer> runsPerPlayer = new HashMap<>();
                List<Entry> ranking = new ArrayList<>();
                for (Map.Entry<UUID, Float> time : times) {
                    UUID uuid = time.getKey();
                    // Times are sorted, so the first entry per player is their personal best
                    if (runsPerPlayer.merge(uuid, 1, Integer::sum) == 1) {
                        String playerName = nameCache.computeIfAbsent(uuid, id -> {
                            String name = Bukkit.getOfflinePlayer(id).getName();
                            return name != null ? name : "Steve";
                        });
                        ranking.add(new Entry(uuid, playerName, time.getValue()));
                    }
                }

                newStats.put(parkourName.toLowerCase(Locale.ROOT), new ParkourStats(
                        parkourName,
                        List.copyOf(ranking),
                        Map.copyOf(runsPerPlayer),
                        times.size()
                ));
            }

            stats = Map.copyOf(newStats);
        } catch (SQLException | IllegalStateException e) {
            plugin.getLogger().warning("Failed to refresh placeholder cache: " + e.getMessage());
        }
    }
}
