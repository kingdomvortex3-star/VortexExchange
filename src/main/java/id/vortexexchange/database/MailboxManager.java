
package id.vortexexchange.database;

import id.vortexexchange.util.ItemSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

    public synchronized void initialize() throws SQLException {
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

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement("""
                         CREATE INDEX IF NOT EXISTS idx_mailbox_player
                         ON mailbox(player_uuid)
                     """)) {
            statement.execute();
        }
    }

    public synchronized boolean store(UUID player, ItemStack item) {
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
            statement.setString(2, ItemSerializer.serialize(item.clone()));
            statement.setLong(3, System.currentTimeMillis());

            return statement.executeUpdate() == 1;

        } catch (SQLException | IllegalArgumentException exception) {
            return false;
        }
    }

    public synchronized List<MailboxItem> getItems(UUID player)
            throws SQLException {

        if (player == null) {
            return new ArrayList<>();
        }

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
                    try {
                        ItemStack item = ItemSerializer.deserialize(
                                result.getString("item_data")
                        );

                        if (item != null && !item.getType().isAir()) {
                            items.add(new MailboxItem(
                                    result.getInt("id"),
                                    item
                            ));
                        }
                    } catch (IllegalArgumentException exception) {
                        // Data rusak tetap disimpan agar bisa diperiksa admin.
                    }
                }
            }
        }

        return items;
    }

    public synchronized boolean deleteItem(int id) throws SQLException {
        String sql = "DELETE FROM mailbox WHERE id = ?";

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public synchronized int getItemCount(UUID player) throws SQLException {
        if (player == null) {
            return 0;
        }

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

    public synchronized void deliver(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        try {
            List<MailboxItem> items = getItems(uuid);

            int delivered = 0;

            for (MailboxItem mailboxItem : items) {
                if (!player.isOnline()) {
                    break;
                }

                ItemStack item = mailboxItem.item();

                if (item == null || item.getType().isAir()) {
                    continue;
                }

                var leftovers = player.getInventory().addItem(item.clone());

                if (leftovers.isEmpty()) {
                    if (deleteItem(mailboxItem.id())) {
                        delivered++;
                    }
                    continue;
                }

                ItemStack remaining = leftovers.values()
                        .iterator()
                        .next();

                if (remaining != null
                        && !remaining.getType().isAir()) {
                    updateItem(mailboxItem.id(), remaining);
                }
            }

            if (delivered > 0 && player.isOnline()) {
                player.sendMessage(
                        "§aMailbox: §f" + delivered
                                + " §aitem berhasil diterima."
                );
            }

        } catch (SQLException exception) {
            player.sendMessage(
                    "§cGagal mengambil barang dari mailbox. "
                            + "Silakan hubungi admin."
            );
        }
    }

    private synchronized boolean updateItem(int id, ItemStack item)
            throws SQLException {

        if (item == null || item.getType().isAir()) {
            return deleteItem(id);
        }

        String sql = """
            UPDATE mailbox
            SET item_data = ?
            WHERE id = ?
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(
                    1,
                    ItemSerializer.serialize(item.clone())
            );

            statement.setInt(2, id);

            return statement.executeUpdate() == 1;
        }
    }

    public record MailboxItem(int id, ItemStack item) {
    }
}
