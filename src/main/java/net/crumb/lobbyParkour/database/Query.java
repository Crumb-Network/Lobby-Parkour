package net.crumb.lobbyParkour.database;

import net.crumb.lobbyParkour.utils.ItemMaker;
import net.crumb.lobbyParkour.utils.LocationHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;

public class Query {
    private final Connection connection;

    public Query(Connection connection) {
        this.connection = connection;
    }

    public void createParkour(String name, Location startLocation, UUID startEntityUuid, Location endLocation, UUID endEntityUuid) throws SQLException {
        String sql = "INSERT INTO parkours (pk_name, start_cp, start_cp_material, start_cp_entity_uuid, end_cp, end_cp_material, end_cp_entity_uuid) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, LocationHelper.locationToString(startLocation));
            statement.setString(3, "minecraft:light_weighted_pressure_plate");
            statement.setString(4, startEntityUuid.toString());
            statement.setString(5, LocationHelper.locationToString(endLocation));
            statement.setString(6, "minecraft:light_weighted_pressure_plate");
            statement.setString(7, endEntityUuid.toString());
            statement.executeUpdate();
        }
    }

    public Location getStartLocation(String parkourName) throws SQLException {
        String sql = "SELECT start_cp FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, parkourName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return LocationHelper.stringToLocation(rs.getString("start_cp"));
            }
        }
        return null;
    }

    public Location getEndLocation(String parkourName) throws SQLException {
        String sql = "SELECT end_cp FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, parkourName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return LocationHelper.stringToLocation(rs.getString("end_cp"));
            }
        }
        return null;
    }

    public ItemStack getStartType(String mapName) throws SQLException {
        String sql = "SELECT start_cp_material FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, mapName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String materialName = rs.getString("start_cp_material").replace("minecraft:", "").toUpperCase();
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) return new ItemStack(material);
                }
            }
        }
        return null;
    }

    public ItemStack getEndType(String mapName) throws SQLException {
        String sql = "SELECT end_cp_material FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, mapName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String materialName = rs.getString("end_cp_material");
                    if (materialName.startsWith("minecraft:")) {
                        materialName = materialName.substring(10);
                    }

                    Material material = Material.matchMaterial(materialName.toUpperCase());
                    if (material != null) {
                        return new ItemStack(material);
                    }
                }
            }
        }
        return null;
    }


    public UUID getStartEntityUuid(String mapName) throws SQLException {
        String sql = "SELECT start_cp_entity_uuid FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, mapName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("start_cp_entity_uuid"));
            }
        }
        return null;
    }

    public UUID getEndEntityUuid(String mapName) throws SQLException {
        String sql = "SELECT end_cp_entity_uuid FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, mapName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("end_cp_entity_uuid"));
            }
        }
        return null;
    }

    public String getMapNameByStartUuid(UUID entityUUID) throws SQLException {
        String sql = "SELECT pk_name FROM parkours WHERE start_cp_entity_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("pk_name");
            }
        }
        return null;
    }

    public String getMapNameByEndUuid(UUID entityUUID) throws SQLException {
        String sql = "SELECT pk_name FROM parkours WHERE end_cp_entity_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("pk_name");
            }
        }
        return null;
    }

    public void updateStartEntityUuid(String mapName, UUID newUuid) throws SQLException {
        String sql = "UPDATE parkours SET start_cp_entity_uuid = ? WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newUuid.toString());
            stmt.setString(2, mapName);
            stmt.executeUpdate();
        }
    }

    public void updateEndEntityUuid(String mapName, UUID newUuid) throws SQLException {
        String sql = "UPDATE parkours SET end_cp_entity_uuid = ? WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newUuid.toString());
            stmt.setString(2, mapName);
            stmt.executeUpdate();
        }
    }

    public void updateStartType(String mapName, String newType) throws SQLException {
        String sql = "UPDATE parkours SET start_cp_material = ? WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newType);
            stmt.setString(2, mapName);
            stmt.executeUpdate();
        }
    }


    public void updateEndType(String mapName, String newType) throws SQLException {
        String sql = "UPDATE parkours SET end_cp_material = ? WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newType);
            stmt.setString(2, mapName);
            stmt.executeUpdate();
        }
    }

    public String getMapnameByPkSpawn(Location location) throws SQLException {
        String sql = "SELECT pk_name FROM parkours WHERE start_cp = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, LocationHelper.locationToString(location));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("pk_name");
            }
        }
        return null;
    }

    public String getMapNameByPkEnd(Location location) throws SQLException {
        String sql = "SELECT pk_name FROM parkours WHERE end_cp = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, LocationHelper.locationToString(location));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("pk_name");
            }
        }
        return null;
    }

    public boolean parkourExists(String name) throws SQLException {
        String sql = "SELECT 1 FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<String> parkourMaps() throws SQLException {
        List<String> parkourNames = new ArrayList<>();
        String sql = "SELECT pk_name FROM parkours";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                parkourNames.add(rs.getString("pk_name"));
            }
        }
        return parkourNames;
    }

    public void renameParkour(String oldName, String newName) throws SQLException {
        String sql = "UPDATE parkours SET pk_name = ? WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newName);
            stmt.setString(2, oldName);
            stmt.executeUpdate();
        }
    }

    public void deleteParkour(String parkourName) throws SQLException {
        String sql = "DELETE FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, parkourName);
            stmt.executeUpdate();
        }
    }

    public List<Object[]> getAllParkourStarts() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT pk_name, start_cp, start_cp_material FROM parkours WHERE start_cp IS NOT NULL";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Location location = LocationHelper.stringToLocation(rs.getString("start_cp"));
                Material material = Material.matchMaterial(rs.getString("start_cp_material").replace("minecraft:", "").toUpperCase());
                if (location != null && material != null) {
                    list.add(new Object[]{rs.getString("pk_name"), location, material});
                }
            }
        }
        return list;
    }

    public List<Object[]> getAllParkourEnds() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT pk_name, end_cp, end_cp_material FROM parkours WHERE end_cp IS NOT NULL";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Location location = LocationHelper.stringToLocation(rs.getString("end_cp"));
                Material material = Material.matchMaterial(rs.getString("end_cp_material").replace("minecraft:", "").toUpperCase());
                if (location != null && material != null) {
                    list.add(new Object[]{rs.getString("pk_name"), location, material});
                }
            }
        }
        return list;
    }

    public int createLeaderboard(int parkourId) throws SQLException {
        String sql = "INSERT INTO leaderboards (parkour_id) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, parkourId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Creating leaderboard failed, no ID returned.");
            }
        }
    }

    public Integer getParkourIdFromName(String parkourName) throws SQLException {
        String sql = "SELECT id FROM parkours WHERE pk_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, parkourName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    return null;
                }
            }
        }
    }

    public void saveTime(UUID uuid, int parkourId, float time) throws SQLException {
        String sql = "INSERT INTO times (uuid, comp_time, parkour_id) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setFloat(2, time);
            stmt.setInt(3, parkourId);
            stmt.executeUpdate();
        }
    }

    public void createCheckpoint(int parkourId, int cp_index, Location location, String material, UUID entityUUID) throws SQLException {
        String sql = "INSERT INTO checkpoints (parkour_id, cp_index, location, material, entity_uuid) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            stmt.setInt(2, cp_index);
            stmt.setString(3, LocationHelper.locationToString(location));
            stmt.setString(4, material);
            stmt.setString(5, entityUUID.toString());
            stmt.executeUpdate();
        }
    }

    public int getMaxCheckpointIndex(int parkourId) throws SQLException {
        String sql = "SELECT MAX(cp_index) AS max_index FROM checkpoints WHERE parkour_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_index");
                }
            }
        }
        return 0;
    }

    public List<UUID> getItemLinesUuid() throws SQLException {
        List<UUID> uuids = new ArrayList<>();
        String sql = "SELECT entity_uuid FROM leaderboard_lines WHERE position = -1";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String uuidString = rs.getString("entity_uuid");
                if (uuidString != null) uuids.add(UUID.fromString(uuidString));
            }
        }
        return uuids;
    }

    public List<Map.Entry<UUID, Integer>> getTitleLines() throws SQLException {
        List<Map.Entry<UUID, Integer>> lines = new ArrayList<>();
        String sql = "SELECT entity_uuid, leaderboard_id FROM leaderboard_lines WHERE position = 0";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String uuidString = rs.getString("entity_uuid");
                int leaderboardId = rs.getInt("leaderboard_id");
                if (uuidString != null) {
                    lines.add(new AbstractMap.SimpleEntry<>(UUID.fromString(uuidString), leaderboardId));
                }
            }
        }
        return lines;
    }

    public List<Map.Entry<UUID, Integer>> getTimesLines(int parkourId) throws SQLException {
        List<Map.Entry<UUID, Integer>> lines = new ArrayList<>();
        String sql = """
        SELECT ll.entity_uuid, ll.position
        FROM leaderboard_lines ll
        JOIN leaderboards lb ON ll.leaderboard_id = lb.id
        WHERE ll.position > 0 AND lb.parkour_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String uuidString = rs.getString("entity_uuid");
                    int position = rs.getInt("position");
                    if (uuidString != null) {
                        lines.add(new AbstractMap.SimpleEntry<>(UUID.fromString(uuidString), position));
                    }
                }
            }
        }

        return lines;
    }

    public Map<Integer, List<Map.Entry<UUID, Integer>>> getAllTimesLinesForParkour(int parkourId) throws SQLException {
        Map<Integer, List<Map.Entry<UUID, Integer>>> result = new HashMap<>();

        String sql = """
        SELECT ll.entity_uuid, ll.position, ll.leaderboard_id
        FROM leaderboard_lines ll
        JOIN leaderboards lb ON ll.leaderboard_id = lb.id
        WHERE lb.parkour_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("entity_uuid"));
                    int position = rs.getInt("position");
                    int leaderboardId = rs.getInt("leaderboard_id");

                    result.computeIfAbsent(leaderboardId, k -> new ArrayList<>())
                            .add(Map.entry(uuid, position));
                }
            }
        }

        return result;
    }


    public List<Map.Entry<UUID, Float>> getParkourTimes(int parkourId) throws SQLException {
        List<Map.Entry<UUID, Float>> times = new ArrayList<>();
        String sql = "SELECT uuid, comp_time FROM times WHERE parkour_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String uuidString = rs.getString("uuid");
                float time = rs.getFloat("comp_time");
                if (uuidString != null) {
                    times.add(new AbstractMap.SimpleEntry<>(UUID.fromString(uuidString), time));
                }
            }
        }
        return times;
    }

    public List<UUID> getLeaderboardLinesByLeaderboardId(int leaderboardId) throws SQLException {
        List<UUID> uuids = new ArrayList<>();
        String sql = "SELECT entity_uuid FROM leaderboard_lines WHERE leaderboard_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leaderboardId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String uuidString = rs.getString("entity_uuid");
                    if (uuidString != null) {
                        uuids.add(UUID.fromString(uuidString));
                    }
                }
            }
        }

        return uuids;
    }

    public void deleteLeaderboardLines(int leaderboardId) throws SQLException {
        String sql = "DELETE FROM leaderboard_lines WHERE leaderboard_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leaderboardId);
            stmt.executeUpdate();
        }
    }

    public void deleteLeaderboard(int leaderboardId) throws SQLException {
        String sql = "DELETE FROM leaderboards WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leaderboardId);
            stmt.executeUpdate();
        }
    }

    public List<Object[]> getCheckpoints(int parkourId) throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT id, cp_index, location, material, entity_uuid FROM checkpoints WHERE parkour_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int cp_index = rs.getInt("cp_index");
                String location = rs.getString("location");
                String material = rs.getString("material");
                UUID entityUUID = UUID.fromString(rs.getString("entity_uuid"));
                list.add(new Object[]{rs.getInt("id"), cp_index, location, material, entityUUID});
            }
        }
        return list;
    }

    public List<Object[]> getCheckpoints() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT id, parkour_id, cp_index, location, material, entity_uuid FROM checkpoints";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int cp_index = rs.getInt("cp_index");
                String location = rs.getString("location");
                String material = rs.getString("material");
                UUID entityUUID = UUID.fromString(rs.getString("entity_uuid"));
                int parkourId = rs.getInt("parkour_id");
                list.add(new Object[]{rs.getInt("id"), cp_index, location, material, entityUUID, parkourId});
            }
        }
        return list;
    }

    public Location getCheckpointLocation(int parkourId, int checkpointIndex) throws SQLException {
        String sql = "SELECT location FROM checkpoints WHERE parkour_id = ? AND cp_index = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            stmt.setInt(2, checkpointIndex);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return LocationHelper.stringToLocation(rs.getString("location"));
                }
            }
        }
        return null;
    }

    public void deleteCheckpoint(int parkourId, int checkpointIndex) throws SQLException {
        String sql = "DELETE FROM checkpoints WHERE parkour_id = ? AND cp_index = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            stmt.setInt(2, checkpointIndex);
            stmt.executeUpdate();
        }
    }

    public void updateLeaderboardLineEntityUuid(UUID oldUuid, UUID newUuid) throws SQLException {
        String sql = "UPDATE leaderboard_lines SET entity_uuid = ? WHERE entity_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newUuid.toString());
            stmt.setString(2, oldUuid.toString());
            stmt.executeUpdate();
        }
    }

    public boolean isCheckpoint(int parkourId) throws SQLException {
        String sql = "SELECT parkour_id, cp_index, location, material FROM checkpoints WHERE parkour_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parkourId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("parkour_id") == parkourId;
                }
            }
        }
        return false;
    }

    public boolean isCheckpoint(Location location) throws SQLException {
        String sql = "SELECT 1 FROM checkpoints WHERE location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, LocationHelper.locationToString(location));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public ItemStack getCheckpointType(String location) throws SQLException {
        String sql = "SELECT material FROM checkpoints WHERE location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return ItemMaker.createItem(rs.getString("material"), 1, "", new ArrayList<>());
                }
            }
        }
        return null;
    }

    public void updateCheckpointType(String location, String newType) throws SQLException {
        String sql = "UPDATE checkpoints SET material = ? WHERE location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newType);
            stmt.setString(2, location);
            stmt.executeUpdate();
        }
    }

    public Map<Location, UUID> getDisplayItemLocations() throws SQLException {
        String sql = "SELECT location, uuid FROM leaderboard_lines WHERE position = -1";
        Map<Location, UUID> result = new HashMap<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String locString = rs.getString("location");
                String uuidString = rs.getString("uuid");

                Location location = LocationHelper.stringToLocation(locString);
                UUID uuid = uuidString != null ? UUID.fromString(uuidString) : null;

                if (location != null) {
                    result.put(location, uuid);
                }
            }
        }
        return result;
    }

    public void updateCheckpointLocation(String location, int parkourId, int checkpointIndex) throws SQLException {
        String sql = "UPDATE checkpoints SET location = ? WHERE parkour_id = ? AND cp_index = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, location);
            stmt.setInt(2, parkourId);
            stmt.setInt(3, checkpointIndex);
            stmt.executeUpdate();
        }
    }

    public void updateCheckPointEntityUUID(String location, UUID entityUUID) throws SQLException {
        String sql = "UPDATE checkpoints SET entity_uuid = ? WHERE location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityUUID.toString());
            stmt.setString(2, location);
            stmt.executeUpdate();
        }
    }

    public int getCheckpointIndex(String location) throws SQLException {
        String sql = "SELECT cp_index FROM checkpoints WHERE location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cp_index");
                }
            }
        }
        return 0;
    }

    public int getParkourIdByCheckpointLocation(String location) throws SQLException {
        String sql = "SELECT parkour_id FROM checkpoints WHERE location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("parkour_id");
                }
            }
        }
        return 0;
    }

    public UUID getCheckpointDisplay(String location) throws SQLException {
        String sql = "SELECT entity_uuid FROM checkpoints WHERE location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, location);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("entity_uuid"));
                }
            }
        }
        return null;
    }

    public String getParkourNameById(int id) throws SQLException {
        String sql = "SELECT pk_name FROM parkours WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("pk_name");
                }
            }
        }
        return null;
    }

    public void removeCheckpoint(int id, int index) throws SQLException {
        String sql = "DELETE FROM checkpoints WHERE parkour_id = ? AND cp_index = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, index);
            stmt.executeUpdate();
        }
    }

    public void updateCheckpointIndex(int parkourId, int oldIndex, int newIndex) throws SQLException {
        String sql = "UPDATE checkpoints SET cp_index = ? WHERE parkour_id = ? AND cp_index = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, newIndex);
            stmt.setInt(2, parkourId);
            stmt.setInt(3, oldIndex);
            stmt.executeUpdate();
        }
    }

    public Map<String, Object> getLeaderboardLineByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT location, position, leaderboard_id FROM leaderboard_lines WHERE entity_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("location", LocationHelper.stringToLocation(rs.getString("location")));
                    result.put("position", rs.getInt("position"));
                    result.put("leaderboard_id", rs.getInt("leaderboard_id"));
                    return result;
                }
            }
        }
        return null;
    }


    public boolean isLeaderboardLine(UUID entityUUID) throws SQLException {
        String sql = "SELECT leaderboard_id FROM leaderboard_lines WHERE entity_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("leaderboard_id") == 1;
                }
            }
            return false;
        }
    }

    public int leaderboardCount() throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM leaderboards";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
            return 0;
        }
    }

    public Map<Integer, String> getLeaderboardNames() throws SQLException {
        Map<Integer, String> leaderboards = new HashMap<>();

        String sql = """
            SELECT l.id AS leaderboard_id, p.pk_name
            FROM leaderboards l
            JOIN parkours p ON p.id = l.parkour_id
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("leaderboard_id");
                String name = rs.getString("pk_name");
                leaderboards.put(id, name + " (#" + id + ")");
            }
        }

        return leaderboards;
    }

    public List<Location> getLeaderboardLocations() throws SQLException {
        List<Location> locations = new ArrayList<>();

        String sql = """
            SELECT ll.location
            FROM leaderboard_lines ll
            JOIN leaderboards l ON l.id = ll.leaderboard_id
            GROUP BY ll.leaderboard_id
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String locStr = rs.getString("location");
                Location loc = LocationHelper.stringToLocation(locStr);
                if (loc != null) {
                    locations.add(loc);
                }
            }
        }
        return locations;
    }

    public int getLeaderboardId(UUID entityUUID) throws SQLException {
        String sql = "SELECT leaderboard_id FROM leaderboard_lines WHERE entity_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, entityUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("leaderboard_id");
                }
            }
            return 0;
        }
    }

    public Location getLeaderboardLocation(int lbId) throws SQLException {
        String sql = "SELECT location FROM leaderboard_lines WHERE leaderboard_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, lbId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return LocationHelper.stringToLocation(rs.getString("location"));
                }
            }
            return null;
        }
    }

    public void createLeaderboardLine(int leaderboardId, Location location, UUID entityUuid, int position) throws SQLException {
        String sql = "INSERT INTO leaderboard_lines (location, entity_uuid, position, leaderboard_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, LocationHelper.locationToString(location));
            stmt.setString(2, entityUuid.toString());
            stmt.setInt(3, position);
            stmt.setInt(4, leaderboardId);
            stmt.executeUpdate();
        }
    }

    public String getParkourNameByLeaderboard(int leaderboardId) throws SQLException {
        String sql = "SELECT pk.pk_name FROM parkours pk JOIN leaderboards lb ON pk.id = lb.parkour_id WHERE lb.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, leaderboardId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("pk_name");
            }
        }
        return null;
    }
}