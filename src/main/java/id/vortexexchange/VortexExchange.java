
package id.vortexexchange;

import id.vortexexchange.auction.AuctionCommand;
import id.vortexexchange.auction.AuctionExpiryManager;
import id.vortexexchange.auction.AuctionListener;
import id.vortexexchange.auction.AuctionManager;
import id.vortexexchange.database.DatabaseManager;
import id.vortexexchange.database.MailboxManager;
import id.vortexexchange.database.MailboxListener;
import id.vortexexchange.database.TradeStorage;
import id.vortexexchange.economy.EconomyManager;
import id.vortexexchange.trade.TradeCommand;
import id.vortexexchange.trade.TradeListener;
import id.vortexexchange.trade.TradeManager;
import id.vortexexchange.trade.TradeRecoveryManager;
import id.vortexexchange.trade.TradeTransactionManager;
import id.vortexexchange.util.LanguageManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;

public final class VortexExchange extends JavaPlugin {

    private static VortexExchange instance;

    private DatabaseManager databaseManager;
    private MailboxManager mailboxManager;
    private TradeStorage tradeStorage;
    private EconomyManager economyManager;
    private AuctionManager auctionManager;
    private AuctionExpiryManager expiryManager;
    private TradeManager tradeManager;
    private TradeRecoveryManager recoveryManager;
    private LanguageManager languageManager;

    private BukkitTask expiryTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        languageManager = new LanguageManager(this);

        getLogger().info("Starting VortexExchange...");

        databaseManager = new DatabaseManager(this);

        try {
            databaseManager.connect();

            mailboxManager = new MailboxManager(databaseManager);
            mailboxManager.initialize();

            tradeStorage = new TradeStorage(databaseManager);

        } catch (SQLException exception) {
            getLogger().severe(
                    "Database gagal diinisialisasi: "
                            + exception.getMessage()
            );

            databaseManager.close();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        economyManager = new EconomyManager(this);

        if (!economyManager.setup()) {
            getLogger().severe(
                    "Vault atau plugin ekonomi tidak ditemukan!"
            );

            databaseManager.close();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(databaseManager);
        expiryManager = new AuctionExpiryManager(this, auctionManager);
        tradeManager = new TradeManager(tradeStorage);

        recoveryManager = new TradeRecoveryManager(
                this,
                tradeManager
        );

        getLogger().info("Memeriksa sesi trade yang tersimpan...");

        int recovered = recoveryManager.recoverAll();

        getLogger().info(
                "Startup trade recovery selesai. Sesi dipulihkan: "
                        + recovered
        );

        if (!registerCommands()) {
            databaseManager.close();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        registerListeners();
        startExpiryTask();

        getLogger().info("--------------------------------");
        getLogger().info("VortexExchange berhasil diaktifkan!");
        getLogger().info("Auction House: ENABLED");
        getLogger().info("Player Trading: ENABLED");
        getLogger().info("Mailbox: ENABLED");
        getLogger().info("Auction Expiry: ENABLED");
        getLogger().info("Trade Storage: ENABLED");
        getLogger().info("Trade Recovery: ENABLED");
        getLogger().info("Language: " +
                getConfig().getString("settings.language", "id"));
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
                        transactionManager,
                        mailboxManager
                ),
                this
        );

        getServer().getPluginManager().registerEvents(
                new MailboxListener(mailboxManager),
                this
        );
    }

    private void startExpiryTask() {
        expiryTask = getServer().getScheduler().runTaskTimer(
                this,
                () -> {
                    try {
                        int count = expiryManager.processExpired();

                        if (count > 0) {
                            getLogger().info(
                                    "Berhasil memproses "
                                            + count
                                            + " listing kedaluwarsa."
                            );
                        }

                    } catch (SQLException exception) {
                        getLogger().severe(
                                "Auction expiry error: "
                                        + exception.getMessage()
                        );
                    }
                },
                100L,
                6000L
        );
    }

    @Override
    public void onDisable() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }

        if (recoveryManager != null) {
            int recovered = recoveryManager.recoverAll();

            getLogger().info(
                    "Shutdown trade recovery selesai. Sesi dipulihkan: "
                            + recovered
            );
        }

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

    public TradeStorage getTradeStorage() {
        return tradeStorage;
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

    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}
