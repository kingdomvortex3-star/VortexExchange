
package id.vortexexchange;

import id.vortexexchange.auction.AuctionCommand;
import id.vortexexchange.auction.AuctionListener;
import id.vortexexchange.auction.AuctionManager;
import id.vortexexchange.database.DatabaseManager;
import id.vortexexchange.database.MailboxManager;
import id.vortexexchange.database.MailboxListener;
import id.vortexexchange.economy.EconomyManager;
import id.vortexexchange.trade.TradeCommand;
import id.vortexexchange.trade.TradeListener;
import id.vortexexchange.trade.TradeManager;
import id.vortexexchange.trade.TradeTransactionManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class VortexExchange extends JavaPlugin {

    private static VortexExchange instance;

    private DatabaseManager databaseManager;
    private MailboxManager mailboxManager;
    private EconomyManager economyManager;
    private AuctionManager auctionManager;
    private TradeManager tradeManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getLogger().info("Starting VortexExchange...");

        databaseManager = new DatabaseManager(this);

        try {
            databaseManager.connect();

            mailboxManager = new MailboxManager(databaseManager);
            mailboxManager.initialize();

        } catch (SQLException exception) {
            getLogger().severe(
                    "Database gagal diinisialisasi: "
                            + exception.getMessage()
            );

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        economyManager = new EconomyManager(this);

        if (!economyManager.setup()) {
            getLogger().severe(
                    "Vault atau plugin ekonomi tidak ditemukan!"
            );

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(databaseManager);
        tradeManager = new TradeManager();

        if (!registerCommands()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerListeners();

        getLogger().info("--------------------------------");
        getLogger().info("VortexExchange berhasil diaktifkan!");
        getLogger().info("Auction House: ENABLED");
        getLogger().info("Player Trading: ENABLED");
        getLogger().info("Mailbox: ENABLED");
        getLogger().info("Economy: Vault");
        getLogger().info("Database: SQLite");
        getLogger().info("--------------------------------");
    }

    private boolean registerCommands() {
        PluginCommand auctionCommand = getCommand("ah");
        PluginCommand tradeCommand = getCommand("trade");

        if (auctionCommand == null || tradeCommand == null) {
            getLogger().severe(
                    "Command ah atau trade tidak ditemukan di plugin.yml!"
            );
            return false;
        }

        auctionCommand.setExecutor(
                new AuctionCommand(this, auctionManager)
        );

        tradeCommand.setExecutor(
                new TradeCommand(this, tradeManager, economyManager)
        );

        return true;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new AuctionListener(
                        this,
                        auctionManager,
                        economyManager
                ),
                this
        );

        TradeTransactionManager transactionManager =
                new TradeTransactionManager(
                        this,
                        economyManager,
                        tradeManager
                );

        getServer().getPluginManager().registerEvents(
                new TradeListener(
                        this,
                        tradeManager,
                        transactionManager
                ),
                this
        );

        getServer().getPluginManager().registerEvents(
                new MailboxListener(mailboxManager),
                this
        );
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("VortexExchange telah dinonaktifkan.");
    }

    public static VortexExchange getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MailboxManager getMailboxManager() {
        return mailboxManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }
}
