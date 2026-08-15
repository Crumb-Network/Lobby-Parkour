package net.crumb.lobbyParkour.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ParkoursDatabase {

    private static ParkoursDatabase instance;

    private final Connection connection;

    private ParkoursDatabase(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        Statement statement = connection.createStatement();

        statement.execute("""
            CREATE TABLE IF NOT EXISTS parkours (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pk_name TEXT NOT NULL,
                end_cp TEXT DEFAULT NULL,
                start_cp TEXT DEFAULT NULL,
                end_cp_material TEXT DEFAULT NULL,
                start_cp_material TEXT DEFAULT NULL,
                start_cp_entity_uuid TEXT DEFAULT NULL,
                end_cp_entity_uuid TEXT DEFAULT NULL
            );
        """);

        statement.execute("""
            CREATE TABLE IF NOT EXISTS checkpoints (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parkour_id INTEGER NOT NULL,
                cp_index INTEGER NOT NULL,
                location TEXT NOT NULL,
                material TEXT NOT NULL,
                entity_uuid TEXT NOT NULL,
                FOREIGN KEY (parkour_id) REFERENCES parkours(id) ON DELETE CASCADE
            );
        """);

        statement.execute("""
            CREATE TABLE IF NOT EXISTS times (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                comp_time REAL NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                parkour_id INTEGER NOT NULL,
                FOREIGN KEY (parkour_id) REFERENCES parkours(id) ON DELETE CASCADE
            );
        """);

        statement.execute("""
            CREATE TABLE IF NOT EXISTS leaderboards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parkour_id INTEGER NOT NULL,
                FOREIGN KEY (parkour_id) REFERENCES parkours(id) ON DELETE CASCADE
            );
        """);

        statement.execute("""
            CREATE TABLE IF NOT EXISTS leaderboard_lines (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                location TEXT NOT NULL,
                entity_uuid TEXT NOT NULL,
                position INTEGER NOT NULL,
                leaderboard_id INTEGER NOT NULL,
                FOREIGN KEY (leaderboard_id) REFERENCES leaderboards(id) ON DELETE CASCADE
            );
        """);

        statement.close();
    }

    public static void init(String path) throws SQLException {
        if (instance != null && instance.connection != null && !instance.connection.isClosed()) {
            return;
        }
        instance = new ParkoursDatabase(path);
    }

    public static Connection getConnection() {
        if (instance == null || instance.connection == null) {
            throw new IllegalStateException(
                    "Database connection not initialised - call ParkoursDatabase.init(path) in onEnable first");
        }
        return instance.connection;
    }

    public static void close() throws SQLException {
        if (instance != null && instance.connection != null && !instance.connection.isClosed()) {
            instance.connection.close();
        }
        instance = null;
    }
}
