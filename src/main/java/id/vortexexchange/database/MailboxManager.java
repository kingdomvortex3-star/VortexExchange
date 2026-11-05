
package id.vortexexchange.database;

import id.vortexexchange.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailboxManager {

    private final DatabaseManager database;

    public MailboxManager(DatabaseManager database) {
        this.database = database;
    }

    public void initialize() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS mailbox (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                item_data TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {
            statement.execute();
        }
    }

    public boolean store(UUID player, ItemStack item) {
        if (player == null || item == null || item.getType().isAir()) {
            return false;
        }

        String sql = """
            INSERT INTO mailbox
            (player_uuid, item_data, created_at)
            VALUES (?, ?, ?)
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, player.toString());
            statement.setString(2, ItemSerializer.serialize(item));
            statement.setLong(3, System.currentTimeMillis());

            return statement.executeUpdate() > 0;

        } catch (SQLException | IllegalArgumentException exception) {
            return false;
        }
    }

    public List<MailboxItem> getItems(UUID player) throws SQLException {
        List<MailboxItem> items = new ArrayList<>();

        String sql = """
            SELECT id, item_data
            FROM mailbox
            WHERE player_uuid = ?
            ORDER BY id ASC
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, player.toString());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    items.add(new MailboxItem(
                            result.getInt("id"),
                            ItemSerializer.deserialize(
                                    result.getString("item_data")
                            )
                    ));
                }
            }
        }

        return items;
    }

    public boolean deleteItem(int id) throws SQLException {
        String sql = "DELETE FROM mailbox WHERE id = ?";

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public int getItemCount(UUID player) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM mailbox
            WHERE player_uuid = ?
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, player.toString());

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    public void deliver(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            List<MailboxItem> items = getItems(player.getUniqueId());

            for (MailboxItem mailboxItem : items) {
                ItemStack item = mailboxItem.item();

                var leftovers = player.getInventory().addItem(item);

                if (leftovers.isEmpty()) {
                    deleteItem(mailboxItem.id());
                } else {
                    ItemStack remaining = leftovers.values()
                            .iterator().next();

                    if (remaining != null) {
                        updateItem(mailboxItem.id(), remaining);
                    }
                }
            }

        } catch (SQLException exception) {
            player.sendMessage("§cGagal mengambil barang dari mailbox.");
        }
    }

    private void updateItem(int id, ItemStack item) throws SQLException {
        String sql = """
            UPDATE mailbox
            SET item_data = ?
            WHERE id = ?
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, ItemSerializer.serialize(item));
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    public record MailboxItem(int id, ItemStack item) {
    }
}
