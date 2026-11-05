
package id.vortexexchange.database;

import id.vortexexchange.util.ItemSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public synchronized boolean store(
            UUID player,
            ItemStack item
    ) {
        if (player == null || !isValidItem(item)) {
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
            statement.setString(
                    2,
                    ItemSerializer.serialize(item.clone())
            );
            statement.setLong(3, System.currentTimeMillis());

            return statement.executeUpdate() == 1;

        } catch (SQLException | RuntimeException exception) {
            return false;
        }
    }

    public synchronized List<MailboxItem> getItems(UUID player)
            throws SQLException {

        List<MailboxItem> items = new ArrayList<>();

        if (player == null) {
            return items;
        }

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

                        if (isValidItem(item)) {
                            items.add(new MailboxItem(
                                    result.getInt("id"),
                                    item
                            ));
                        }

                    } catch (RuntimeException exception) {
                        // Data rusak tetap disimpan untuk pemeriksaan admin.
                    }
                }
            }
        }

        return items;
    }

    public synchronized boolean deleteItem(int id)
            throws SQLException {

        String sql = "DELETE FROM mailbox WHERE id = ?";

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    public synchronized int getItemCount(UUID player)
            throws SQLException {

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

        try {
            List<MailboxItem> items = getItems(
                    player.getUniqueId()
            );

            int delivered = 0;

            for (MailboxItem mailboxItem : items) {
                if (!player.isOnline()) {
                    break;
                }

                ItemStack item = mailboxItem.item();

                if (!isValidItem(item)) {
                    continue;
                }

                ItemStack[] inventoryBefore =
                        player.getInventory().getStorageContents();

                Map<Integer, ItemStack> leftovers =
                        player.getInventory().addItem(item.clone());

                boolean saved;

                if (leftovers.isEmpty()) {
                    saved = deleteItem(mailboxItem.id());
                } else {
                    saved = saveLeftovers(
                            mailboxItem.id(),
                            leftovers
                    );
                }

                if (!saved) {
                    player.getInventory().setStorageContents(
                            inventoryBefore
                    );

                    break;
                }

                delivered++;
            }

            if (delivered > 0 && player.isOnline()) {
                player.sendMessage(
                        "§aMailbox: §f" + delivered
                                + " §aitem berhasil diterima."
                );
            }

        } catch (SQLException | RuntimeException exception) {
            player.sendMessage(
                    "§cGagal mengambil barang dari Mailbox. "
                            + "Silakan hubungi admin."
            );
        }
    }

    private boolean saveLeftovers(
            int id,
            Map<Integer, ItemStack> leftovers
    ) throws SQLException {

        if (leftovers == null || leftovers.isEmpty()) {
            return deleteItem(id);
        }

        List<ItemStack> remainingItems =
                new ArrayList<>(leftovers.values());

        for (ItemStack item : remainingItems) {
            if (!isValidItem(item)) {
                return false;
            }
        }

        ItemStack first = remainingItems.get(0);

        if (!updateItem(id, first)) {
            return false;
        }

        for (int i = 1; i < remainingItems.size(); i++) {
            if (!store(
                    getOwnerUUID(id),
                    remainingItems.get(i)
            )) {
                return false;
            }
        }

        return true;
    }

    private UUID getOwnerUUID(int id) throws SQLException {
        String sql = """
            SELECT player_uuid
            FROM mailbox
            WHERE id = ?
        """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException(
                            "Data Mailbox tidak ditemukan: " + id
                    );
                }

                try {
                    return UUID.fromString(
                            result.getString("player_uuid")
                    );
                } catch (IllegalArgumentException exception) {
                    throw new SQLException(
                            "UUID Mailbox tidak valid.",
                            exception
                    );
                }
            }
        }
    }

    private synchronized boolean updateItem(
            int id,
            ItemStack item
    ) throws SQLException {

        if (!isValidItem(item)) {
            return false;
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

    private boolean isValidItem(ItemStack item) {
        return item != null
                && !item.getType().isAir()
                && item.getAmount() > 0;
    }

    public record MailboxItem(int id, ItemStack item) {

        public MailboxItem {
            if (id <= 0 || item == null) {
                throw new IllegalArgumentException(
                        "Data Mailbox tidak valid."
                );
            }

            item = item.clone();
        }

        @Override
        public ItemStack item() {
            return item.clone();
        }
    }
}
