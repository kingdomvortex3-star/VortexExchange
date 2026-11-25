
package id.vortexexchange.auction;

import id.vortexexchange.database.DatabaseManager;
import id.vortexexchange.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManager {

    private final DatabaseManager database;

    public AuctionManager(DatabaseManager database) {
        this.database = database;
    }

    public synchronized int createListing(
            UUID sellerUUID,
            String sellerName,
            ItemStack item,
            double price,
            long durationMillis
    ) throws SQLException {

        if (sellerUUID == null
                || sellerName == null
                || sellerName.isBlank()
                || item == null
                || item.getType().isAir()
                || !Double.isFinite(price)
                || price <= 0
                || durationMillis <= 0) {

            throw new IllegalArgumentException(
                    "Data listing tidak valid."
            );
        }

        long createdAt = System.currentTimeMillis();
        long expiresAt;

        try {
            expiresAt = Math.addExact(createdAt, durationMillis);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Durasi listing terlalu panjang.",
                    exception
            );
        }

        String sql = """
            INSERT INTO auctions
            (seller_uuid, seller_name, item_data, price, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setString(1, sellerUUID.toString());
            statement.setString(2, sellerName);
            statement.setString(
                    3,
                    ItemSerializer.serialize(item.clone())
            );
            statement.setDouble(4, price);
            statement.setLong(5, createdAt);
            statement.setLong(6, expiresAt);

            if (statement.executeUpdate() != 1) {
                throw new SQLException("Listing gagal disimpan.");
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             "SELECT last_insert_rowid()"
                     );
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt(1);
            }
        }

        throw new SQLException("Gagal mendapatkan ID listing.");
    }

    public List<AuctionListing> getListings() throws SQLException {

        List<AuctionListing> listings = new ArrayList<>();

        String sql = """
            SELECT *
            FROM auctions
            WHERE expires_at > ?
            ORDER BY created_at DESC
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setLong(1, System.currentTimeMillis());

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    AuctionListing listing = readListing(result);

                    if (listing != null) {
                        listings.add(listing);
                    }
                }
            }
        }

        return listings;
    }

    public AuctionListing getListing(int id) throws SQLException {

        String sql = """
            SELECT *
            FROM auctions
            WHERE id = ?
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                return readListing(result);
            }
        }
    }

    public boolean deleteListing(int id) throws SQLException {

        String sql = "DELETE FROM auctions WHERE id = ?";

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);

            return statement.executeUpdate() == 1;
        }
    }

    /**
     * Membatalkan listing dan memasukkan barang ke Mailbox.
     *
     * Item dan listing diproses dalam satu transaksi SQLite.
     * Jika salah satu proses gagal, seluruh transaksi di-rollback.
     */
    public synchronized boolean cancelListingToMailbox(
            int listingId,
            UUID sellerUUID
    ) throws SQLException {

        if (listingId <= 0 || sellerUUID == null) {
            return false;
        }

        Connection connection = database.getConnection();

        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database tidak tersedia.");
        }

        boolean previousAutoCommit = connection.getAutoCommit();

        try {

            connection.setAutoCommit(false);

            String itemData;

            // 1. Pastikan listing benar-benar milik pemain.
            String selectSQL = """
                SELECT item_data
                FROM auctions
                WHERE id = ?
                  AND seller_uuid = ?
                  AND expires_at > ?
            """;

            try (PreparedStatement select =
                         connection.prepareStatement(selectSQL)) {

                select.setInt(1, listingId);
                select.setString(2, sellerUUID.toString());
                select.setLong(3, System.currentTimeMillis());

                try (ResultSet result = select.executeQuery()) {

                    if (!result.next()) {
                        connection.commit();
                        return false;
                    }

                    itemData = result.getString("item_data");
                }
            }

            if (itemData == null || itemData.isBlank()) {
                throw new SQLException(
                        "Data item listing kosong: " + listingId
                );
            }

            // 2. Masukkan barang ke Mailbox.
            String insertSQL = """
                INSERT INTO mailbox
                (player_uuid, item_data, created_at)
                VALUES (?, ?, ?)
            """;

            try (PreparedStatement insert =
                         connection.prepareStatement(insertSQL)) {

                insert.setString(1, sellerUUID.toString());
                insert.setString(2, itemData);
                insert.setLong(3, System.currentTimeMillis());

                if (insert.executeUpdate() != 1) {
                    throw new SQLException(
                            "Gagal menyimpan item ke Mailbox."
                    );
                }
            }

            // 3. Hapus listing setelah item berhasil masuk Mailbox.
            String deleteSQL = """
                DELETE FROM auctions
                WHERE id = ?
                  AND seller_uuid = ?
                  AND expires_at > ?
            """;

            try (PreparedStatement delete =
                         connection.prepareStatement(deleteSQL)) {

                delete.setInt(1, listingId);
                delete.setString(2, sellerUUID.toString());
                delete.setLong(3, System.currentTimeMillis());

                if (delete.executeUpdate() != 1) {
                    throw new SQLException(
                            "Listing gagal dihapus: " + listingId
                    );
                }
            }

            // 4. Simpan seluruh perubahan.
            connection.commit();

            return true;

        } catch (SQLException | RuntimeException exception) {

            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }

            if (exception instanceof SQLException sqlException) {
                throw sqlException;
            }

            throw new SQLException(
                    "Gagal membatalkan listing: " + listingId,
                    exception
            );

        } finally {

            connection.setAutoCommit(previousAutoCommit);
        }
    }

    public int getPlayerListingCount(UUID sellerUUID)
            throws SQLException {

        if (sellerUUID == null) {
            return 0;
        }

        String sql = """
            SELECT COUNT(*)
            FROM auctions
            WHERE seller_uuid = ?
              AND expires_at > ?
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, sellerUUID.toString());
            statement.setLong(2, System.currentTimeMillis());

            try (ResultSet result = statement.executeQuery()) {

                return result.next()
                        ? result.getInt(1)
                        : 0;
            }
        }
    }

    public Connection getConnection() {
        return database.getConnection();
    }

    private AuctionListing readListing(ResultSet result)
            throws SQLException {

        try {

            ItemStack item = ItemSerializer.deserialize(
                    result.getString("item_data")
            );

            if (item == null || item.getType().isAir()) {
                return null;
            }

            return new AuctionListing(
                    result.getInt("id"),
                    UUID.fromString(result.getString("seller_uuid")),
                    result.getString("seller_name"),
                    item,
                    result.getDouble("price"),
                    result.getLong("created_at"),
                    result.getLong("expires_at")
            );

        } catch (IllegalArgumentException exception) {

            return null;
        }
    }
}
