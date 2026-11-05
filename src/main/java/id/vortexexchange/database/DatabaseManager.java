
package id.vortexexchange.database;

import id.vortexexchange.VortexExchange;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final VortexExchange plugin;
    private volatile Connection connection;

    public DatabaseManager(VortexExchange plugin) {
        this.plugin = plugin;
    }

    public synchronized void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        File folder = plugin.getDataFolder();

        if (!folder.exists() && !folder.mkdirs()) {
            throw new SQLException(
                    "Tidak dapat membuat folder plugin."
            );
        }

        File database = new File(folder, "data.db");

        Connection newConnection = DriverManager.getConnection(
                "jdbc:sqlite:" + database.getAbsolutePath()
        );

        try {
            configure(newConnection);
            createTables(newConnection);

            connection = newConnection;

            plugin.getLogger().info(
                    "Database SQLite berhasil terhubung."
            );

        } catch (SQLException exception) {
            try {
                newConnection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }

            throw exception;
        }
    }

    private void configure(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
    }

    private void createTables(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement()) {

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

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_auctions_expiry
                ON auctions(expires_at)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_auctions_seller_expiry
                ON auctions(seller_uuid, expires_at)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_auctions_created
                ON auctions(created_at DESC)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_trade_history_player_one
                ON trade_history(player_one)
            """);

            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_trade_history_player_two
                ON trade_history(player_two)
            """);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }

            plugin.getLogger().info(
                    "Database SQLite berhasil ditutup."
            );

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Gagal menutup database: "
                            + exception.getMessage()
            );

        } finally {
            connection = null;
        }
    }
}
