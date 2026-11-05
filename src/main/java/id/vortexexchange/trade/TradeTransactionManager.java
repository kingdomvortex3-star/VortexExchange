
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
                || !session.bothReady()) {
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

        if (!economy.has(one, oneMoney)
                || !economy.has(two, twoMoney)) {
            return fail(session, "Saldo salah satu pemain tidak cukup.");
        }

        List<ItemStack> oneItems = session.getItems(oneId);
        List<ItemStack> twoItems = session.getItems(twoId);

        if (!withdraw(one, oneMoney)) {
            return fail(session, "Gagal mengambil uang pemain pertama.");
        }

        if (!withdraw(two, twoMoney)) {
            if (!refund(one, oneMoney)) {
                logRecovery(oneId, oneMoney);
            }

            return fail(session, "Gagal mengambil uang pemain kedua.");
        }

        if (!deposit(one, twoMoney)) {
            boolean refundOne = refund(one, oneMoney);
            boolean refundTwo = refund(two, twoMoney);

            if (!refundOne) {
                logRecovery(oneId, oneMoney);
            }

            if (!refundTwo) {
                logRecovery(twoId, twoMoney);
            }

            return fail(session, "Pembayaran pertama gagal.");
        }

        if (!deposit(two, oneMoney)) {
            boolean reversePayment = withdraw(one, twoMoney);

            if (reversePayment) {
                boolean refundOne = refund(one, oneMoney);
                boolean refundTwo = refund(two, twoMoney);

                if (!refundOne) {
                    logRecovery(oneId, oneMoney);
                }

                if (!refundTwo) {
                    logRecovery(twoId, twoMoney);
                }
            } else {
                boolean refundTwo = refund(two, twoMoney);

                plugin.getLogger().severe(
                        "ROLLBACK TRADE GAGAL! "
                                + "Pemain pertama: " + oneId
                                + ", pemain kedua: " + twoId
                                + ", uang pertama: " + oneMoney
                                + ", uang kedua: " + twoMoney
                );

                if (!refundTwo) {
                    logRecovery(twoId, twoMoney);
                }
            }

            return fail(
                    session,
                    "Pembayaran kedua gagal. Periksa saldo dan hubungi admin jika berubah."
            );
        }

        tradeManager.completeSession(session);

        deliverItems(one, twoItems);
        deliverItems(two, oneItems);

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

    private boolean withdraw(Player player, double amount) {
        return amount == 0 || economy.withdraw(player, amount);
    }

    private boolean deposit(Player player, double amount) {
        return amount == 0 || economy.deposit(player, amount);
    }

    private boolean refund(Player player, double amount) {
        return amount == 0 || economy.deposit(player, amount);
    }

    private void deliverItems(
            Player receiver,
            List<ItemStack> items
    ) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            var leftovers = receiver.getInventory().addItem(item.clone());

            leftovers.values().forEach(leftover ->
                    receiver.getWorld().dropItemNaturally(
                            receiver.getLocation(),
                            leftover
                    )
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

    private void logRecovery(UUID player, double amount) {
        plugin.getLogger().severe(
                "REFUND TRADE PERLU PEMERIKSAAN MANUAL: "
                        + "player=" + player
                        + ", amount=" + amount
        );
    }
}
