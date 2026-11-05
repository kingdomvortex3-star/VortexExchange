
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
        if (session == null || !session.isActive()
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
                || oneMoney < 0 || twoMoney < 0) {
            return fail(session, "Penawaran uang tidak valid.");
        }

        if (!economy.has(one, oneMoney)
                || !economy.has(two, twoMoney)) {
            return fail(session, "Saldo salah satu pemain tidak cukup.");
        }

        List<ItemStack> oneItems = session.getItems(oneId);
        List<ItemStack> twoItems = session.getItems(twoId);

        if (!economy.withdraw(one, oneMoney)) {
            return fail(session, "Gagal mengambil uang pemain pertama.");
        }

        if (!economy.withdraw(two, twoMoney)) {
            if (!refund(one, oneMoney)) {
                logRecovery(oneId, oneMoney);
            }

            return fail(session, "Gagal mengambil uang pemain kedua.");
        }

        boolean firstDeposit = economy.deposit(one, twoMoney);

        if (!firstDeposit) {
            refund(one, oneMoney);
            refund(two, twoMoney);
            return fail(session, "Gagal mengirim uang kepada pemain pertama.");
        }

        boolean secondDeposit = economy.deposit(two, oneMoney);

        if (!secondDeposit) {
            boolean rollbackOne = economy.withdraw(one, twoMoney);
            boolean rollbackTwo = economy.deposit(one, oneMoney);
            boolean rollbackThree = economy.deposit(two, twoMoney);

            if (!rollbackOne || !rollbackTwo || !rollbackThree) {
                plugin.getLogger().severe(
                        "ROLLBACK TRADE GAGAL! Perlu pemeriksaan manual. "
                                + oneId + " <-> " + twoId
                );
            }

            return fail(session, "Pembayaran gagal. Periksa saldo jika berubah.");
        }

        tradeItems(one, twoItems);
        tradeItems(two, oneItems);

        tradeManager.completeSession(session);

        one.sendMessage("§a§lTRADE BERHASIL!");
        two.sendMessage("§a§lTRADE BERHASIL!");

        one.sendMessage("§7Transaksi dengan §b" + two.getName() + " §7selesai.");
        two.sendMessage("§7Transaksi dengan §b" + one.getName() + " §7selesai.");

        one.closeInventory();
        two.closeInventory();

        return true;
    }

    private void tradeItems(Player receiver, List<ItemStack> items) {
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

    private boolean refund(Player player, double amount) {
        return amount <= 0 || economy.deposit(player, amount);
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
                "GAGAL REFUND TRADE: player=" + player
                        + ", amount=" + amount
        );
    }
}
