
package id.vortexexchange.economy;

import id.vortexexchange.VortexExchange;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final VortexExchange plugin;
    private Economy economy;

    public EconomyManager(VortexExchange plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager()
                .getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                plugin.getServer().getServicesManager()
                        .getRegistration(Economy.class);

        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();

        return economy != null;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double getBalance(OfflinePlayer player) {
        if (economy == null || player == null) {
            return 0.0;
        }

        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy != null
                && player != null
                && amount >= 0
                && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isValid(player, amount) || !has(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isValid(player, amount)) {
            return false;
        }

        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String format(double amount) {
        if (economy == null) {
            return String.format("%.2f", amount);
        }

        return economy.format(amount);
    }

    private boolean isValid(OfflinePlayer player, double amount) {
        return economy != null
                && player != null
                && Double.isFinite(amount)
                && amount > 0;
    }
}
