
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.database.MailboxManager;
import id.vortexexchange.economy.EconomyManager;
import id.vortexexchange.util.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class TradeTransactionManager {

    private final VortexExchange plugin;
    private final EconomyManager economy;
    private final TradeManager tradeManager;
    private final MailboxManager mailboxManager;

    public TradeTransactionManager(
            VortexExchange plugin,
            EconomyManager economy,
            TradeManager tradeManager
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.tradeManager = tradeManager;
        this.mailboxManager = plugin.getMailboxManager();
    }

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    public synchronized boolean complete(TradeSession session) {
        if (session == null
                || !session.isActive()
                || !session.areBothReady()) {
            return false;
        }

        UUID oneId = session.getPlayerOne();
        UUID twoId = session.getPlayerTwo();

        Player one = Bukkit.getPlayer(oneId);
        Player two = Bukkit.getPlayer(twoId);

        if (one == null || two == null
                || !one.isOnline() || !two.isOnline()) {
            return fail(session, "trade.player-offline");
        }

        if (!economy.isAvailable()) {
            return fail(session, "economy.missing");
        }

        double oneMoney = session.getMoney(oneId);
        double twoMoney = session.getMoney(twoId);

        if (!Double.isFinite(oneMoney)
                || !Double.isFinite(twoMoney)
                || oneMoney < 0
                || twoMoney < 0) {
            return fail(session, "trade.invalid-money");
        }

        List<ItemStack> oneItems = session.getItems(oneId);
        List<ItemStack> twoItems = session.getItems(twoId);

        if (!validateItems(oneItems) || !validateItems(twoItems)) {
            return fail(session, "trade.invalid-items");
        }

        if (!economy.has(one, oneMoney)
                || !economy.has(two, twoMoney)) {
            return fail(session, "trade.insufficient-money");
        }

        // Withdraw both offers first.
        if (!withdraw(one, oneMoney)) {
            return fail(session, "trade.payment-failed");
        }

        if (!withdraw(two, twoMoney)) {
            if (!refund(one, oneMoney)) {
                logRecovery(oneId, oneMoney, "Refund pertama gagal.");
            }

            return fail(session, "trade.payment-failed");
        }

        // Deposit the first payment.
        if (!deposit(one, twoMoney)) {
            boolean refundOne = refund(one, oneMoney);
            boolean refundTwo = refund(two, twoMoney);

            logRefundFailures(
                    oneId, twoId,
                    oneMoney, twoMoney,
                    refundOne, refundTwo
            );

            return fail(session, "trade.payment-failed");
        }

        // Deposit the second payment.
        if (!deposit(two, oneMoney)) {
            // Reverse the first deposit before refunding withdrawals.
            boolean reversePayment = withdraw(one, twoMoney);

            if (!reversePayment) {
                logRecovery(
                        oneId,
                        twoMoney,
                        "Gagal membatalkan pembayaran pertama."
                );

                plugin.getLogger().severe(
                        "Rollback tidak aman. Refund manual diperlukan."
                );

                return fail(session, "trade.rollback-failed");
            }

            boolean refundOne = refund(one, oneMoney);
            boolean refundTwo = refund(two, twoMoney);

            logRefundFailures(
                    oneId, twoId,
                    oneMoney, twoMoney,
                    refundOne, refundTwo
            );

            return fail(session, "trade.payment-failed");
        }

        // Deliver items. Overflow is stored in the recipient's mailbox.
        boolean firstDelivery = deliverItems(twoId, one, twoItems);
        boolean secondDelivery = deliverItems(oneId, two, oneItems);

        if (!firstDelivery || !secondDelivery) {
            plugin.getLogger().severe(
                    "TRADE DELIVERY ERROR! "
                            + "PlayerOne=" + oneId
                            + ", PlayerTwo=" + twoId
            );

            one.sendMessage(lang().get("trade.delivery-error"));
            two.sendMessage(lang().get("trade.delivery-error"));

            // Keep the session stored for administrator investigation.
            return false;
        }

        tradeManager.completeSession(session);

        if (!tradeManager.deleteStoredSession(session)) {
            plugin.getLogger().severe(
                    "GAGAL MENGHAPUS DATA TRADE SELESAI: "
                            + oneId + " <-> " + twoId
            );
        }

        one.sendMessage(lang().get("trade.completed"));
        two.sendMessage(lang().get("trade.completed"));

        one.sendMessage(lang().get(
                "trade.completed-with",
                "player", two.getName()
        ));

        two.sendMessage(lang().get(
                "trade.completed-with",
                "player", one.getName()
        ));

        one.closeInventory();
        two.closeInventory();

        plugin.getLogger().info(
                "Trade berhasil: " + oneId + " <-> " + twoId
        );

        return true;
    }

    private boolean validateItems(List<ItemStack> items) {
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

    private boolean withdraw(Player player, double amount) {
        return amount == 0 || economy.withdraw(player, amount);
    }

    private boolean deposit(Player player, double amount) {
        return amount == 0 || economy.deposit(player, amount);
    }

    private boolean refund(Player player, double amount) {
        return amount == 0 || economy.deposit(player, amount);
    }

    private boolean deliverItems(
            UUID owner,
            Player receiver,
            List<ItemStack> items
    ) {
        try {
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir()) {
                    continue;
                }

                var leftovers = receiver.getInventory().addItem(
                        item.clone()
                );

                for (ItemStack leftover : leftovers.values()) {
                    if (!mailboxManager.store(owner, leftover.clone())) {
                        plugin.getLogger().severe(
                                "GAGAL MENYIMPAN SISA ITEM TRADE KE MAILBOX: "
                                        + owner
                        );
                        return false;
                    }
                }
            }

            return true;

        } catch (RuntimeException exception) {
            plugin.getLogger().severe(
                    "Gagal mengirim item trade kepada "
                            + receiver.getUniqueId()
                            + ": " + exception.getMessage()
            );

            return false;
        }
    }

    private void logRefundFailures(
            UUID oneId,
            UUID twoId,
            double oneMoney,
            double twoMoney,
            boolean refundOne,
            boolean refundTwo
    ) {
        if (!refundOne) {
            logRecovery(
                    oneId,
                    oneMoney,
                    "Refund pemain pertama gagal."
            );
        }

        if (!refundTwo) {
            logRecovery(
                    twoId,
                    twoMoney,
                    "Refund pemain kedua gagal."
            );
        }
    }

    private boolean fail(TradeSession session, String key) {
        Player one = Bukkit.getPlayer(session.getPlayerOne());
        Player two = Bukkit.getPlayer(session.getPlayerTwo());

        if (one != null) {
            one.sendMessage(lang().get(key));
        }

        if (two != null) {
            two.sendMessage(lang().get(key));
        }

        return false;
    }

    private void logRecovery(
            UUID player,
            double amount,
            String reason
    ) {
        plugin.getLogger().severe(
                "TRADE MEMERLUKAN PEMERIKSAAN MANUAL! "
                        + "Player=" + player
                        + ", Amount=" + amount
                        + ", Reason=" + reason
        );
    }
}
