
package id.vortexexchange.auction;

import id.vortexexchange.VortexExchange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionExpiryManager {

    private final VortexExchange plugin;
    private final AuctionManager auctionManager;

    public AuctionExpiryManager(
            VortexExchange plugin,
            AuctionManager auctionManager
    ) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    public synchronized int processExpired() throws SQLException {
        Connection connection = auctionManager.getConnection();

        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database tidak tersedia.");
        }

        boolean returnItems = plugin.getConfig().getBoolean(
                "auction.expired-items-return", true
        );

        boolean previousAutoCommit = connection.getAutoCommit();

        if (!previousAutoCommit) {
            throw new SQLException(
                    "Database sedang menjalankan transaksi lain."
            );
        }

        long currentTime = System.currentTimeMillis();

        List<ExpiredListing> expired = new ArrayList<>();

        try {
            connection.setAutoCommit(false);

            String selectSQL = """
                SELECT id, seller_uuid, item_data
                FROM auctions
                WHERE expires_at <= ?
                ORDER BY id ASC
            """;

            try (PreparedStatement statement =
                         connection.prepareStatement(selectSQL)) {

                statement.setLong(1, currentTime);

                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        String sellerUUID =
                                result.getString("seller_uuid");

                        String itemData =
                                result.getString("item_data");

                        if (sellerUUID == null || sellerUUID.isBlank()
                                || itemData == null || itemData.isBlank()) {
                            throw new SQLException(
                                    "Data listing kedaluwarsa tidak valid. ID: "
                                            + result.getInt("id")
                            );
                        }

                        expired.add(new ExpiredListing(
                                result.getInt("id"),
                                sellerUUID,
                                itemData
                        ));
                    }
                }
            }

            if (expired.isEmpty()) {
                connection.commit();
                return 0;
            }

            String insertSQL = """
                INSERT INTO mailbox
                (player_uuid, item_data, created_at)
                VALUES (?, ?, ?)
            """;

            String deleteSQL = """
                DELETE FROM auctions
                WHERE id = ? AND expires_at <= ?
            """;

            int processed = 0;

            try (PreparedStatement insert =
                         connection.prepareStatement(insertSQL);
                 PreparedStatement delete =
                         connection.prepareStatement(deleteSQL)) {

                for (ExpiredListing listing : expired) {

                    if (returnItems) {
                        insert.setString(1, listing.sellerUUID());
                        insert.setString(2, listing.itemData());
                        insert.setLong(3, currentTime);

                        if (insert.executeUpdate() != 1) {
                            throw new SQLException(
                                    "Gagal memasukkan barang ke Mailbox. ID: "
                                            + listing.id()
                            );
                        }
                    }

                    delete.setInt(1, listing.id());
                    delete.setLong(2, currentTime);

                    if (delete.executeUpdate() != 1) {
                        throw new SQLException(
                                "Gagal menghapus listing. ID: "
                                        + listing.id()
                        );
                    }

                    processed++;
                }
            }

            connection.commit();

            if (returnItems) {
                plugin.getLogger().info(
                        processed
                                + " listing kedaluwarsa dipindahkan ke Mailbox."
                );
            } else {
                plugin.getLogger().info(
                        processed
                                + " listing kedaluwarsa telah dihapus. "
                                + "Pengembalian barang dinonaktifkan."
                );
            }

            return processed;

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
                    "Gagal memproses listing kedaluwarsa.",
                    exception
            );

        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private record ExpiredListing(
            int id,
            String sellerUUID,
            String itemData
    ) {
    }
}
