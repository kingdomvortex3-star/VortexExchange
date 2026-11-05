
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

    public int processExpired() throws SQLException {
        if (!plugin.getConfig().getBoolean(
                "auction.expired-items-return", true
        )) {
            return 0;
        }

        Connection connection = auctionManager.getConnection();

        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database tidak tersedia.");
        }

        List<ExpiredListing> expired = new ArrayList<>();

        String selectSQL = """
            SELECT id, seller_uuid, item_data
            FROM auctions
            WHERE expires_at <= ?
            ORDER BY id ASC
        """;

        try (PreparedStatement statement =
                     connection.prepareStatement(selectSQL)) {

            statement.setLong(1, System.currentTimeMillis());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    expired.add(new ExpiredListing(
                            result.getInt("id"),
                            result.getString("seller_uuid"),
                            result.getString("item_data")
                    ));
                }
            }
        }

        if (expired.isEmpty()) {
            return 0;
        }

        boolean previousAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

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
                    insert.setString(1, listing.sellerUUID());
                    insert.setString(2, listing.itemData());
                    insert.setLong(3, System.currentTimeMillis());

                    insert.executeUpdate();

                    delete.setInt(1, listing.id());
                    delete.setLong(2, System.currentTimeMillis());

                    if (delete.executeUpdate() != 1) {
                        throw new SQLException(
                                "Gagal menghapus listing " + listing.id()
                        );
                    }

                    processed++;
                }
            }

            connection.commit();

            plugin.getLogger().info(
                    processed + " listing kedaluwarsa dipindahkan ke Mailbox."
            );

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
