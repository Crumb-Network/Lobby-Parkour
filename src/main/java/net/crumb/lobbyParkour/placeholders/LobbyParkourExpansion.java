package net.crumb.lobbyParkour.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.systems.LeaderboardUpdater;
import net.crumb.lobbyParkour.systems.ParkourSession;
import net.crumb.lobbyParkour.systems.ParkourSessionManager;
import net.crumb.lobbyParkour.systems.PlaceholderCache;
import net.crumb.lobbyParkour.systems.PlaceholderCache.Entry;
import net.crumb.lobbyParkour.systems.PlaceholderCache.ParkourStats;
import net.crumb.lobbyParkour.utils.ConfigManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PlaceholderAPI expansion for Lobby Parkour.
 *
 * <p>Parkour placeholders: {@code %lobbyparkour_pk_<name>_<stat>%}
 * <ul>
 *     <li>{@code top<N>}, {@code top<N>_name}, {@code top<N>_time}, {@code top<N>_seconds}</li>
 *     <li>{@code completions}, {@code players}</li>
 *     <li>{@code best}, {@code best_seconds}, {@code position}, {@code runs} (requesting player)</li>
 * </ul>
 *
 * <p>Player placeholders: {@code in_parkour}, {@code current_parkour}, {@code current_time},
 * {@code current_seconds}, {@code current_checkpoint}, {@code current_checkpoint_total},
 * {@code completions}, {@code parkour_count}
 */
public class LobbyParkourExpansion extends PlaceholderExpansion {
    private static final Pattern TOP_PATTERN = Pattern.compile("top(\\d+)(?:_(name|time|seconds))?");
    private static final Set<String> PARKOUR_STATS = Set.of(
            "completions", "players", "best", "best_seconds", "position", "runs"
    );

    private final LobbyParkour plugin;

    public LobbyParkourExpansion(LobbyParkour plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "lobbyparkour";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String lower = params.toLowerCase(Locale.ROOT);

        if (lower.startsWith("pk_")) {
            return onParkourRequest(player, params.substring(3));
        }

        return switch (lower) {
            case "in_parkour" -> String.valueOf(getSession(player) != null);
            case "current_parkour" -> {
                ParkourSession session = getSession(player);
                yield session != null ? session.getParkourName() : ConfigManager.getPlaceholders().getEmptyName();
            }
            case "current_time" -> {
                ParkourSession session = getSession(player);
                yield session != null ? LeaderboardUpdater.formatTimer(session.getElapsedSeconds()) : ConfigManager.getPlaceholders().getEmptyTime();
            }
            case "current_seconds" -> {
                ParkourSession session = getSession(player);
                yield session != null ? formatSeconds(session.getElapsedSeconds()) : "0";
            }
            case "current_checkpoint" -> {
                ParkourSession session = getSession(player);
                yield session != null ? String.valueOf(session.getLastReachedCheckpointIndex()) : "0";
            }
            case "current_checkpoint_total" -> {
                ParkourSession session = getSession(player);
                yield session != null ? String.valueOf(session.getMaxCheckpoints()) : "0";
            }
            case "completions" -> {
                if (player == null) yield "0";
                int total = 0;
                for (ParkourStats stats : PlaceholderCache.getAll()) {
                    total += stats.runsPerPlayer().getOrDefault(player.getUniqueId(), 0);
                }
                yield String.valueOf(total);
            }
            case "parkour_count" -> String.valueOf(PlaceholderCache.getAll().size());
            default -> null;
        };
    }

    /**
     * Parkour names can contain underscores, so the name/stat split is found by trying every
     * underscore from the right and picking the first split that names a known parkour.
     */
    private String onParkourRequest(OfflinePlayer player, String params) {
        for (int i = params.lastIndexOf('_'); i > 0; i = params.lastIndexOf('_', i - 1)) {
            String stat = params.substring(i + 1).toLowerCase(Locale.ROOT);
            if (!isParkourStat(stat)) continue;

            ParkourStats stats = PlaceholderCache.get(params.substring(0, i));
            if (stats != null) {
                return resolveParkourStat(player, stats, stat);
            }
        }
        // Unknown parkour or stat: return null so the raw placeholder stays visible for debugging
        return null;
    }

    private static boolean isParkourStat(String stat) {
        return PARKOUR_STATS.contains(stat) || TOP_PATTERN.matcher(stat).matches();
    }

    private String resolveParkourStat(OfflinePlayer player, ParkourStats stats, String stat) {
        ConfigManager.Placeholders config = ConfigManager.getPlaceholders();

        Matcher top = TOP_PATTERN.matcher(stat);
        if (top.matches()) {
            int position;
            try {
                position = Integer.parseInt(top.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
            Entry entry = stats.getEntry(position);
            String type = top.group(2);

            if (type == null) {
                String name = entry != null ? entry.playerName() : config.getEmptyName();
                String time = entry != null ? LeaderboardUpdater.formatTimer(entry.time()) : config.getEmptyTime();
                return config.getTopFormat()
                        .replace("%position%", String.valueOf(position))
                        .replace("%player_name%", name)
                        .replace("%timer%", time);
            }
            return switch (type) {
                case "name" -> entry != null ? entry.playerName() : config.getEmptyName();
                case "time" -> entry != null ? LeaderboardUpdater.formatTimer(entry.time()) : config.getEmptyTime();
                case "seconds" -> entry != null ? formatSeconds(entry.time()) : "0";
                default -> null;
            };
        }

        return switch (stat) {
            case "completions" -> String.valueOf(stats.totalRuns());
            case "players" -> String.valueOf(stats.ranking().size());
            case "runs" -> player == null ? "0"
                    : String.valueOf(stats.runsPerPlayer().getOrDefault(player.getUniqueId(), 0));
            case "position" -> {
                int position = player == null ? -1 : stats.getPosition(player.getUniqueId());
                yield position > 0 ? String.valueOf(position) : config.getEmptyPosition();
            }
            case "best", "best_seconds" -> {
                int position = player == null ? -1 : stats.getPosition(player.getUniqueId());
                if (position < 0) yield stat.equals("best") ? config.getEmptyTime() : "0";
                float time = stats.getEntry(position).time();
                yield stat.equals("best") ? LeaderboardUpdater.formatTimer(time) : formatSeconds(time);
            }
            default -> null;
        };
    }

    private static ParkourSession getSession(OfflinePlayer player) {
        return player == null ? null : ParkourSessionManager.getSession(player.getUniqueId());
    }

    private static String formatSeconds(float seconds) {
        return String.format(Locale.ROOT, "%.2f", seconds);
    }
}
