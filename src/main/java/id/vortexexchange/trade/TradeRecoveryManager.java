
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class TradeRecoveryManager {

    private final VortexExchange plugin;
    private final TradeManager tradeManager;

    public TradeRecoveryManager(
            VortexExchange plugin,
            TradeManager tradeManager
    ) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
    }

    public synchronized int recoverAll() {
        if (plugin.getDatabaseManager() == null) {
            plugin.getLogger().severe(
                    "Trade recovery gagal: database tidak tersedia."
            );
            return 0;
        }

        Connection connection;

        try {
            connection = plugin.getDatabaseManager().getConnection();
        } catch (RuntimeException exception) {
            plugin.getLogger().severe(
                    "Trade recovery gagal mengambil koneksi: "
                            + exception.getMessage()
            );
            return 0;
        }

        if (connection == null) {
            plugin.getLogger().severe(
                    "Trade recovery gagal: koneksi database null."
            );
            return 0;
        }

        int recovered = 0;

        for (TradeSession session : tradeManager.getActiveSessions()) {
            if (recoverSession(connection, session)) {
                recovered++;
            }
        }

        plugin.getLogger().info(
                "Trade recovery selesai. Sesi dipulihkan: " + recovered
        );

        return recovered;
    }

    private synchronized boolean recoverSession(
            Connection connection,
            TradeSession session
    ) {
        if (session == null || !session.isActive()) {
            return false;
        }

        UUID one = session.getPlayerOne();
        UUID two = session.getPlayerTwo();

        List<ItemStack> oneItems = session.getItems(one);
        List<ItemStack> twoItems = session.getItems(two);

        boolean previousAutoCommit;

        try {
            previousAutoCommit = connection.getAutoCommit();
        } catch (SQLException exception) {
            logFailure(one, two, exception);
            return false;
        }

        try {
            connection.setAutoCommit(false);

            String sql = """
                INSERT INTO mailbox
                (player_uuid, item_data, created_at)
                VALUES (?, ?, ?)
            """;

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                saveItems(statement, one, oneItems);
                saveItems(statement, two, twoItems);
            }

            connection.commit();

            session.setCancelled(true);
            tradeManager.removeSession(session);

            plugin.getLogger().info(
                    "Trade recovery berhasil: "
                            + one + " <-> " + two
            );

            return true;

        } catch (Exception exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }

            logFailure(one, two, exception);
            return false;

        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException exception) {
                plugin.getLogger().severe(
                        "Gagal mengembalikan mode transaksi database: "
                                + exception.getMessage()
                );
            }
        }
    }

    private void saveItems(
            PreparedStatement statement,
            UUID player,
            List<ItemStack> items
    ) throws Exception {

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            statement.setString(1, player.toString());
            statement.setString(
                    2,
                    ItemSerializer.serialize(item.clone())
            );
            statement.setLong(3, System.currentTimeMillis());

            statement.executeUpdate();
        }
    }

    private void logFailure(
            UUID one,
            UUID two,
            Exception exception
    ) {
        plugin.getLogger().severe(
                "TRADE RECOVERY GAGAL untuk "
                        + one + " dan " + two
                        + ": " + exception.getMessage()
        );
    }
}
