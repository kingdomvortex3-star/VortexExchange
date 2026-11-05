
package id.vortexexchange.database;

import id.vortexexchange.VortexExchange;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final VortexExchange plugin;
    private Connection connection;

    public DatabaseManager(VortexExchange plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        File folder = plugin.getDataFolder();

        if (!folder.exists() && !folder.mkdirs()) {
            throw new SQLException("Tidak dapat membuat folder plugin.");
        }

        File database = new File(folder, "data.db");

        connection = DriverManager.getConnection(
                "jdbc:sqlite:" + database.getAbsolutePath()
        );

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");

            statement.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    item_data TEXT NOT NULL,
                    price REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS trade_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_one TEXT NOT NULL,
                    player_two TEXT NOT NULL,
                    money_one REAL NOT NULL,
                    money_two REAL NOT NULL,
                    completed_at INTEGER NOT NULL
                )
            """);
        }

        plugin.getLogger().info("Database SQLite berhasil terhubung.");
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
            plugin.getLogger().info("Database SQLite berhasil ditutup.");
        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Gagal menutup database: " + exception.getMessage()
            );
        }
    }
}
