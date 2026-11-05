
package id.vortexexchange;

import org.bukkit.plugin.java.JavaPlugin;

public final class VortexExchange extends JavaPlugin {

    private static VortexExchange instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getLogger().info("VortexExchange berhasil diaktifkan!");
        getLogger().info("Auction House dan Player Trading sedang disiapkan.");
    }

    @Override
    public void onDisable() {
        getLogger().info("VortexExchange berhasil dinonaktifkan.");
    }

    public static VortexExchange getInstance() {
        return instance;
    }
}
