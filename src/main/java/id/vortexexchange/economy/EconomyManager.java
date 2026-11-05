
package id.vortexexchange.economy;

import id.vortexexchange.VortexExchange;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final VortexExchange plugin;
    private volatile Economy economy;

    public EconomyManager(VortexExchange plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager()
                .getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault tidak ditemukan.");
            economy = null;
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                plugin.getServer().getServicesManager()
                        .getRegistration(Economy.class);

        if (provider == null || provider.getProvider() == null) {
            plugin.getLogger().severe(
                    "Provider ekonomi tidak ditemukan. Pastikan EssentialsX Economy aktif."
            );
            economy = null;
            return false;
        }

        economy = provider.getProvider();

        plugin.getLogger().info(
                "Economy provider berhasil terhubung: "
                        + economy.getName()
        );

        return true;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isAvailable() || player == null) {
            return 0.0;
        }

        try {
            double balance = economy.getBalance(player);

            return Double.isFinite(balance) && balance >= 0
                    ? balance
                    : 0.0;
        } catch (RuntimeException exception) {
            plugin.getLogger().severe(
                    "Gagal mengambil saldo pemain: " + exception.getMessage()
            );
            return 0.0;
        }
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!isValid(player, amount)) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        try {
            return economy.has(player, amount);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe(
                    "Gagal memeriksa saldo: " + exception.getMessage()
            );
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isValid(player, amount)) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        try {
            if (!economy.has(player, amount)) {
                return false;
            }

            EconomyResponse response =
                    economy.withdrawPlayer(player, amount);

            return response != null && response.transactionSuccess();
        } catch (RuntimeException exception) {
            plugin.getLogger().severe(
                    "Gagal menarik uang: " + exception.getMessage()
            );
            return false;
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isValid(player, amount)) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        try {
            EconomyResponse response =
                    economy.depositPlayer(player, amount);

            return response != null && response.transactionSuccess();
        } catch (RuntimeException exception) {
            plugin.getLogger().severe(
                    "Gagal menambahkan uang: " + exception.getMessage()
            );
            return false;
        }
    }

    public String format(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            return "Invalid";
        }

        if (!isAvailable()) {
            return String.format(java.util.Locale.US, "%.2f", amount);
        }

        try {
            return economy.format(amount);
        } catch (RuntimeException exception) {
            return String.format(java.util.Locale.US, "%.2f", amount);
        }
    }

    private boolean isValid(OfflinePlayer player, double amount) {
        return economy != null
                && player != null
                && Double.isFinite(amount)
                && amount >= 0;
    }
}
