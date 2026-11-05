
package id.vortexexchange.database;

import id.vortexexchange.trade.TradeSession;
import id.vortexexchange.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TradeStorage {

    private final DatabaseManager databaseManager;

    public TradeStorage(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException(
                    "DatabaseManager tidak boleh null."
            );
        }

        this.databaseManager = databaseManager;
    }

    public synchronized void save(TradeSession session)
            throws SQLException {

        if (session == null || !session.isActive()) {
            throw new SQLException("Sesi trade tidak valid.");
        }

        UUID one = session.getPlayerOne();
        UUID two = session.getPlayerTwo();

        String sessionId = createSessionId(one, two);

        String sql = """
            INSERT INTO active_trades (
                session_id,
                player_one,
                player_two,
                player_one_items,
                player_two_items,
                money_one,
                money_two,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(session_id) DO UPDATE SET
                player_one_items = excluded.player_one_items,
                player_two_items = excluded.player_two_items,
                money_one = excluded.money_one,
                money_two = excluded.money_two,
                updated_at = excluded.updated_at
        """;

        Connection connection = getConnection();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, sessionId);
            statement.setString(2, one.toString());
            statement.setString(3, two.toString());
            statement.setString(4, serializeItems(session.getItems(one)));
            statement.setString(5, serializeItems(session.getItems(two)));
            statement.setDouble(6, session.getMoney(one));
            statement.setDouble(7, session.getMoney(two));
            statement.setLong(8, System.currentTimeMillis());

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Gagal menyimpan sesi trade."
                );
            }
        }
    }

    public synchronized List<StoredTrade> loadAll()
            throws SQLException {

        String sql = """
            SELECT
                player_one,
                player_two,
                player_one_items,
                player_two_items,
                money_one,
                money_two
            FROM active_trades
            ORDER BY updated_at ASC
        """;

        List<StoredTrade> trades = new ArrayList<>();

        Connection connection = getConnection();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                try {
                    UUID one = UUID.fromString(
                            result.getString("player_one")
                    );

                    UUID two = UUID.fromString(
                            result.getString("player_two")
                    );

                    if (one.equals(two)) {
                        throw new IllegalArgumentException(
                                "UUID pemain sama."
                        );
                    }

                    double moneyOne = result.getDouble("money_one");
                    double moneyTwo = result.getDouble("money_two");

                    if (!Double.isFinite(moneyOne)
                            || !Double.isFinite(moneyTwo)
                            || moneyOne < 0
                            || moneyTwo < 0) {
                        throw new IllegalArgumentException(
                                "Jumlah uang tidak valid."
                        );
                    }

                    trades.add(new StoredTrade(
                            one,
                            two,
                            deserializeItems(
                                    result.getString("player_one_items")
                            ),
                            deserializeItems(
                                    result.getString("player_two_items")
                            ),
                            moneyOne,
                            moneyTwo
                    ));

                } catch (IllegalArgumentException exception) {
                    throw new SQLException(
                            "Data sesi trade rusak: "
                                    + exception.getMessage(),
                            exception
                    );
                }
            }
        }

        return trades;
    }

    public synchronized boolean delete(UUID one, UUID two)
            throws SQLException {

        if (one == null || two == null || one.equals(two)) {
            throw new SQLException("UUID trade tidak valid.");
        }

        String sql = """
            DELETE FROM active_trades
            WHERE session_id = ?
        """;

        Connection connection = getConnection();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, createSessionId(one, two));

            return statement.executeUpdate() > 0;
        }
    }

    public synchronized boolean delete(TradeSession session)
            throws SQLException {

        if (session == null) {
            return false;
        }

        return delete(
                session.getPlayerOne(),
                session.getPlayerTwo()
        );
    }

    private String serializeItems(List<ItemStack> items)
            throws SQLException {

        if (items == null || items.isEmpty()) {
            return "";
        }

        List<String> serialized = new ArrayList<>();

        try {
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir()) {
                    continue;
                }

                serialized.add(
                        ItemSerializer.serialize(item.clone())
                );
            }
        } catch (RuntimeException exception) {
            throw new SQLException(
                    "Gagal mengubah item menjadi data database.",
                    exception
            );
        }

        return String.join("\n", serialized);
    }

    private List<ItemStack> deserializeItems(String data)
            throws SQLException {

        List<ItemStack> items = new ArrayList<>();

        if (data == null || data.isBlank()) {
            return items;
        }

        try {
            for (String serialized : data.split("\n")) {
                if (serialized.isBlank()) {
                    continue;
                }

                ItemStack item = ItemSerializer.deserialize(serialized);

                if (item == null || item.getType().isAir()) {
                    throw new IllegalArgumentException(
                            "Item hasil deserialisasi tidak valid."
                    );
                }

                items.add(item);
            }
        } catch (RuntimeException exception) {
            throw new SQLException(
                    "Gagal membaca item dari database.",
                    exception
            );
        }

        return items;
    }

    private Connection getConnection() throws SQLException {
        Connection connection = databaseManager.getConnection();

        if (connection == null || connection.isClosed()) {
            throw new SQLException(
                    "Koneksi database tidak tersedia."
            );
        }

        return connection;
    }

    private String createSessionId(UUID one, UUID two) {
        List<UUID> players = new ArrayList<>(List.of(one, two));
        players.sort(Comparator.comparing(UUID::toString));

        String value = players.get(0) + ":" + players.get(1);

        return UUID.nameUUIDFromBytes(
                value.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    public static final class StoredTrade {

        private final UUID playerOne;
        private final UUID playerTwo;
        private final List<ItemStack> playerOneItems;
        private final List<ItemStack> playerTwoItems;
        private final double moneyOne;
        private final double moneyTwo;

        public StoredTrade(
                UUID playerOne,
                UUID playerTwo,
                List<ItemStack> playerOneItems,
                List<ItemStack> playerTwoItems,
                double moneyOne,
                double moneyTwo
        ) {
            this.playerOne = playerOne;
            this.playerTwo = playerTwo;
            this.playerOneItems = cloneItems(playerOneItems);
            this.playerTwoItems = cloneItems(playerTwoItems);
            this.moneyOne = moneyOne;
            this.moneyTwo = moneyTwo;
        }

        public UUID getPlayerOne() {
            return playerOne;
        }

        public UUID getPlayerTwo() {
            return playerTwo;
        }

        public List<ItemStack> getPlayerOneItems() {
            return cloneItems(playerOneItems);
        }

        public List<ItemStack> getPlayerTwoItems() {
            return cloneItems(playerTwoItems);
        }

        public double getMoneyOne() {
            return moneyOne;
        }

        public double getMoneyTwo() {
            return moneyTwo;
        }

        private static List<ItemStack> cloneItems(
                List<ItemStack> source
        ) {
            List<ItemStack> copy = new ArrayList<>();

            if (source != null) {
                for (ItemStack item : source) {
                    if (item != null) {
                        copy.add(item.clone());
                    }
                }
            }

            return copy;
        }
    }
}
