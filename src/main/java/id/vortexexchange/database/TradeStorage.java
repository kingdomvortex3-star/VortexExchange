
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.database.MailboxManager;
import id.vortexexchange.database.TradeStorage;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class TradeRecoveryManager {

    private final VortexExchange plugin;
    private final TradeManager tradeManager;
    private final TradeStorage tradeStorage;
    private final MailboxManager mailboxManager;

    public TradeRecoveryManager(
            VortexExchange plugin,
            TradeManager tradeManager
    ) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.tradeStorage = plugin.getTradeStorage();
        this.mailboxManager = plugin.getMailboxManager();
    }

    public synchronized int recoverAll() {
        if (tradeStorage == null || mailboxManager == null) {
            plugin.getLogger().severe(
                    "Trade recovery gagal: storage atau mailbox tidak tersedia."
            );
            return 0;
        }

        List<TradeStorage.StoredTrade> storedTrades;

        try {
            storedTrades = tradeStorage.loadAll();
        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Gagal membaca sesi trade: "
                            + exception.getMessage()
            );
            return 0;
        }

        int recovered = 0;

        for (TradeStorage.StoredTrade trade : storedTrades) {
            if (trade != null && recoverStoredTrade(trade)) {
                recovered++;
            }
        }

        plugin.getLogger().info(
                "Trade recovery selesai: "
                        + recovered + "/" + storedTrades.size()
                        + " sesi berhasil dipulihkan."
        );

        return recovered;
    }

    private boolean recoverStoredTrade(
            TradeStorage.StoredTrade trade
    ) {
        UUID one = trade.getPlayerOne();
        UUID two = trade.getPlayerTwo();

        if (one == null || two == null || one.equals(two)) {
            plugin.getLogger().severe(
                    "Recovery gagal: UUID pemain tidak valid."
            );
            return false;
        }

        List<ItemStack> oneItems = trade.getPlayerOneItems();
        List<ItemStack> twoItems = trade.getPlayerTwoItems();

        if (!validItems(oneItems) || !validItems(twoItems)) {
            plugin.getLogger().severe(
                    "Recovery gagal: data item tidak valid. "
                            + one + " <-> " + two
            );
            return false;
        }

        Connection connection = null;
        boolean previousAutoCommit = true;

        try {
            connection = plugin.getDatabaseManager().getConnection();

            if (connection == null || connection.isClosed()) {
                throw new SQLException(
                        "Koneksi database tidak tersedia."
                );
            }

            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            // Simpan item pemain pertama.
            for (ItemStack item : oneItems) {
                if (!mailboxManager.store(one, item.clone())) {
                    throw new SQLException(
                            "Gagal menyimpan item pemain pertama."
                    );
                }
            }

            // Simpan item pemain kedua.
            for (ItemStack item : twoItems) {
                if (!mailboxManager.store(two, item.clone())) {
                    throw new SQLException(
                            "Gagal menyimpan item pemain kedua."
                    );
                }
            }

            // Hapus sesi hanya setelah semua item berhasil disimpan.
            if (!tradeStorage.delete(one, two)) {
                throw new SQLException(
                        "Sesi trade tidak berhasil dihapus."
                );
            }

            connection.commit();

            tradeManager.removeRequests(one);
            tradeManager.removeRequests(two);

            plugin.getLogger().info(
                    "Trade recovery berhasil: "
                            + one + " <-> " + two
            );

            return true;

        } catch (SQLException | RuntimeException exception) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    plugin.getLogger().severe(
                            "Rollback recovery gagal: "
                                    + rollbackException.getMessage()
                    );
                }
            }

            plugin.getLogger().severe(
                    "TRADE RECOVERY GAGAL! "
                            + "PlayerOne=" + one
                            + ", PlayerTwo=" + two
                            + ", Error=" + exception.getMessage()
            );

            return false;

        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(previousAutoCommit);
                } catch (SQLException exception) {
                    plugin.getLogger().severe(
                            "Gagal mengembalikan mode AutoCommit: "
                                    + exception.getMessage()
                    );
                }
            }
        }
    }

    private boolean validItems(List<ItemStack> items) {
        if (items == null) {
            return false;
        }

        for (ItemStack item : items) {
            if (item == null
                    || item.getType().isAir()
                    || item.getAmount() <= 0) {
                return false;
            }
        }

        return true;
    }
}
