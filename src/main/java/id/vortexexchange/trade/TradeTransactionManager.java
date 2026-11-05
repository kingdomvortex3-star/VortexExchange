
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class TradeTransactionManager {

    private final VortexExchange plugin;
    private final EconomyManager economy;
    private final TradeManager tradeManager;

    public TradeTransactionManager(
            VortexExchange plugin,
            EconomyManager economy,
            TradeManager tradeManager
    ) {
        this.plugin = plugin;
        this.economy = economy;
        this.tradeManager = tradeManager;
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
            return fail(session, "Salah satu pemain offline.");
        }

        if (!economy.isAvailable()) {
            return fail(session, "Ekonomi Vault tidak tersedia.");
        }

        double oneMoney = session.getMoney(oneId);
        double twoMoney = session.getMoney(twoId);

        if (!Double.isFinite(oneMoney)
                || !Double.isFinite(twoMoney)
                || oneMoney < 0
                || twoMoney < 0) {
            return fail(session, "Penawaran uang tidak valid.");
        }

        List<ItemStack> oneItems = session.getItems(oneId);
        List<ItemStack> twoItems = session.getItems(twoId);

        if (!validateItems(oneItems) || !validateItems(twoItems)) {
            return fail(session, "Data barang trade tidak valid.");
        }

        if (!economy.has(one, oneMoney)
                || !economy.has(two, twoMoney)) {
            return fail(session, "Saldo salah satu pemain tidak cukup.");
        }

        if (!withdraw(one, oneMoney)) {
            return fail(session, "Gagal mengambil uang pemain pertama.");
        }

        if (!withdraw(two, twoMoney)) {
            boolean refunded = refund(one, oneMoney);

            if (!refunded) {
                logRecovery(
                        oneId,
                        oneMoney,
                        "Refund pemain pertama setelah penarikan kedua gagal."
                );
            }

            return fail(session, "Gagal mengambil uang pemain kedua.");
        }

        if (!deposit(one, twoMoney)) {
            boolean refundOne = refund(one, oneMoney);
            boolean refundTwo = refund(two, twoMoney);

            logRefundFailures(
                    oneId, twoId,
                    oneMoney, twoMoney,
                    refundOne, refundTwo
            );

            return fail(session, "Pembayaran pemain pertama gagal.");
        }

        if (!deposit(two, oneMoney)) {
            rollbackSecondPayment(
                    one, two,
                    oneId, twoId,
                    oneMoney, twoMoney
            );

            return fail(
                    session,
                    "Pembayaran kedua gagal. Hubungi admin jika saldo bermasalah."
            );
        }

        boolean firstDelivery = deliverItems(one, twoItems);
        boolean secondDelivery = deliverItems(two, oneItems);

        if (!firstDelivery || !secondDelivery) {
            plugin.getLogger().severe(
                    "TRADE ITEM DELIVERY ERROR! "
                            + "PlayerOne=" + oneId
                            + ", PlayerTwo=" + twoId
            );

            one.sendMessage(
                    "§cPengiriman item bermasalah. Hubungi admin."
            );

            two.sendMessage(
                    "§cPengiriman item bermasalah. Hubungi admin."
            );

            return false;
        }

        tradeManager.completeSession(session);

        if (!tradeManager.deleteStoredSession(session)) {
            plugin.getLogger().severe(
                    "GAGAL MENGHAPUS DATA TRADE SELESAI DARI DATABASE! "
                            + oneId + " <-> " + twoId
            );
        }

        one.sendMessage("§a§lTRADE BERHASIL!");
        two.sendMessage("§a§lTRADE BERHASIL!");

        one.sendMessage(
                "§7Transaksi dengan §b" + two.getName() + " §7selesai."
        );

        two.sendMessage(
                "§7Transaksi dengan §b" + one.getName() + " §7selesai."
        );

        one.closeInventory();
        two.closeInventory();

        plugin.getLogger().info(
                "Trade berhasil: " + oneId + " <-> " + twoId
        );

        return true;
    }

    private void rollbackSecondPayment(
            Player one,
            Player two,
            UUID oneId,
            UUID twoId,
            double oneMoney,
            double twoMoney
    ) {
        boolean reverseWithdraw = withdraw(one, twoMoney);
        boolean reverseDeposit = false;

        if (reverseWithdraw) {
            reverseDeposit = deposit(two, twoMoney);
        }

        boolean refundOne = refund(one, oneMoney);
        boolean refundTwo = refund(two, twoMoney);

        logRefundFailures(
                oneId, twoId,
                oneMoney, twoMoney,
                refundOne, refundTwo
        );

        if (!reverseWithdraw || !reverseDeposit) {
            plugin.getLogger().severe(
                    "ROLLBACK TRADE TIDAK SEPENUHNYA BERHASIL! "
                            + "Perlu pemeriksaan saldo manual."
            );
        }
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
                    receiver.getWorld().dropItemNaturally(
                            receiver.getLocation(),
                            leftover
                    );
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

    private boolean fail(TradeSession session, String reason) {
        Player one = Bukkit.getPlayer(session.getPlayerOne());
        Player two = Bukkit.getPlayer(session.getPlayerTwo());

        if (one != null) {
            one.sendMessage("§cTrade gagal: " + reason);
        }

        if (two != null) {
            two.sendMessage("§cTrade gagal: " + reason);
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
