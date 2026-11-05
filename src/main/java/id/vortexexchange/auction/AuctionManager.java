
package id.vortexexchange.auction;

import id.vortexexchange.database.DatabaseManager;
import id.vortexexchange.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManager {

    private final DatabaseManager database;

    public AuctionManager(DatabaseManager database) {
        this.database = database;
    }

    public int createListing(
            UUID sellerUUID,
            String sellerName,
            ItemStack item,
            double price,
            long durationMillis
    ) throws SQLException {

        if (sellerUUID == null || sellerName == null
                || item == null || item.getType().isAir()
                || !Double.isFinite(price) || price <= 0
                || durationMillis <= 0) {
            throw new IllegalArgumentException("Data listing tidak valid.");
        }

        long createdAt = System.currentTimeMillis();
        long expiresAt = Math.addExact(createdAt, durationMillis);

        String sql = """
            INSERT INTO auctions
            (seller_uuid, seller_name, item_data, price, created_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, sellerUUID.toString());
            statement.setString(2, sellerName);
            statement.setString(3, ItemSerializer.serialize(item));
            statement.setDouble(4, price);
            statement.setLong(5, createdAt);
            statement.setLong(6, expiresAt);

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException("Gagal mendapatkan ID listing.");
    }

    public List<AuctionListing> getListings() throws SQLException {
        List<AuctionListing> listings = new ArrayList<>();

        String sql = """
            SELECT * FROM auctions
            ORDER BY created_at DESC
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                listings.add(new AuctionListing(
                        result.getInt("id"),
                        UUID.fromString(result.getString("seller_uuid")),
                        result.getString("seller_name"),
                        ItemSerializer.deserialize(
                                result.getString("item_data")
                        ),
                        result.getDouble("price"),
                        result.getLong("created_at"),
                        result.getLong("expires_at")
                ));
            }
        }

        return listings;
    }

    public AuctionListing getListing(int id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                return new AuctionListing(
                        result.getInt("id"),
                        UUID.fromString(result.getString("seller_uuid")),
                        result.getString("seller_name"),
                        ItemSerializer.deserialize(
                                result.getString("item_data")
                        ),
                        result.getDouble("price"),
                        result.getLong("created_at"),
                        result.getLong("expires_at")
                );
            }
        }
    }

    public boolean deleteListing(int id) throws SQLException {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);

            return statement.executeUpdate() > 0;
        }
    }

    public Connection getConnection() {
        return database.getConnection();
    }
}
