
package id.vortexexchange.auction;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
import id.vortexexchange.util.LanguageManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionListener implements Listener {

    private final VortexExchange plugin;
    private final AuctionManager auctionManager;
    private final EconomyManager economyManager;

    private final Set<Integer> processing =
            ConcurrentHashMap.newKeySet();

    public AuctionListener(
            VortexExchange plugin,
            AuctionManager auctionManager,
            EconomyManager economyManager
    ) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.economyManager = economyManager;
    }

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    // ==========================================
    // INVENTORY CLICK
    // ==========================================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof AuctionGUI gui)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(
                        event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getRawSlot();

        // Close Auction House
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        // Prevent unsafe inventory interactions
        if (event.getClick().isShiftClick()
                || event.getClick().isKeyboardClick()
                || event.getClick().isCreativeAction()) {
            return;
        }

        // Previous page
        if (slot == 45) {
            changePage(player, gui, -1);
            return;
        }

        // Next page
        if (slot == 53) {
            changePage(player, gui, 1);
            return;
        }

        // Find listing
        Integer listingId = gui.getListingId(slot);

        if (listingId == null) {
            return;
        }

        // Purchase listing
        purchase(player, listingId);
    }

    // ==========================================
    // PAGINATION
    // ==========================================

    private void changePage(
            Player player,
            AuctionGUI currentGUI,
            int direction
    ) {

        if (player == null || !player.isOnline()) {
            return;
        }

        int newPage = currentGUI.getPage() + direction;

        // Check page boundaries
        if (newPage < 0
                || newPage >= currentGUI.getTotalPages()) {
            return;
        }

        try {

            // Reload active listings
            var listings = auctionManager.getListings();

            // Create new page
            AuctionGUI newGUI = new AuctionGUI(
                    listings,
                    newPage
            );

            // Open GUI on next server tick
            Bukkit.getScheduler().runTask(plugin, () -> {

                if (!player.isOnline()) {
                    return;
                }

                player.openInventory(newGUI.getInventory());
                player.updateInventory();

            });

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Failed to load Auction House page: "
                            + exception.getMessage()
            );

            player.sendMessage(
                    ChatColor.RED
                            + "Gagal memuat halaman Auction House."
            );
        }
    }

    // ==========================================
    // INVENTORY DRAG
    // ==========================================

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory().getHolder()
                instanceof AuctionGUI) {

            event.setCancelled(true);
        }
    }

    // ==========================================
    // PURCHASE LISTING
    // ==========================================

    private void purchase(Player buyer, int listingId) {

        if (!processing.add(listingId)) {
            buyer.sendMessage(lang().get("auction.processing"));
            return;
        }

        try {

            if (!plugin.getConfig().getBoolean(
                    "auction.enabled", true)) {

                buyer.sendMessage(lang().get("auction.disabled"));
                return;
            }

            if (!economyManager.isAvailable()) {
                buyer.sendMessage(lang().get("economy.missing"));
                return;
            }

            AuctionListing listing =
                    auctionManager.getListing(listingId);

            if (listing == null || listing.isExpired()) {
                buyer.sendMessage(lang().get("auction.unavailable"));
                buyer.closeInventory();
                return;
            }

            UUID sellerUUID = listing.getSellerUUID();

            if (sellerUUID == null) {
                buyer.sendMessage(lang().get("auction.invalid-seller"));
                return;
            }

            if (sellerUUID.equals(buyer.getUniqueId())) {
                buyer.sendMessage(lang().get("auction.own-item"));
                return;
            }

            ItemStack item = listing.getItem();

            if (item == null
                    || item.getType().isAir()
                    || item.getAmount() <= 0) {

                buyer.sendMessage(lang().get("auction.invalid-item"));

                plugin.getLogger().severe(
                        "Invalid auction item. Listing ID: " + listingId
                );

                return;
            }

            double price = listing.getPrice();

            if (!Double.isFinite(price) || price <= 0) {
                buyer.sendMessage(lang().get("auction.invalid-price"));
                return;
            }

            if (!economyManager.has(buyer, price)) {
                buyer.sendMessage(lang().get("auction.no-balance"));
                return;
            }

            OfflinePlayer seller =
                    Bukkit.getOfflinePlayer(sellerUUID);

            // Withdraw buyer money
            if (!economyManager.withdraw(buyer, price)) {
                buyer.sendMessage(lang().get("auction.payment-failed"));
                return;
            }

            boolean deleted;

            try {

                deleted = auctionManager.deleteListing(listingId);

            } catch (SQLException exception) {

                refundBuyer(
                        buyer,
                        listingId,
                        price,
                        "Failed to delete listing: "
                                + exception.getMessage()
                );

                buyer.sendMessage(
                        lang().get("auction.transaction-error")
                );

                return;
            }

            if (!deleted) {

                refundBuyer(
                        buyer,
                        listingId,
                        price,
                        "Listing was not deleted."
                );

                buyer.sendMessage(lang().get("auction.unavailable"));
                return;
            }

            // Deposit seller money
            if (!economyManager.deposit(seller, price)) {

                boolean restored = restoreListing(listing);

                boolean refunded = economyManager.deposit(
                        buyer,
                        price
                );

                logRecovery(
                        listingId,
                        buyer.getUniqueId(),
                        price,
                        "Seller payment failed. Listing restored="
                                + restored
                                + ", buyer refunded=" + refunded
                );

                buyer.sendMessage(
                        lang().get("auction.transaction-error")
                );

                return;
            }

            // Deliver purchased item
            try {

                ItemStack purchasedItem = item.clone();

                var leftovers = buyer.getInventory().addItem(
                        purchasedItem
                );

                leftovers.values().forEach(leftover ->
                        buyer.getWorld().dropItemNaturally(
                                buyer.getLocation(),
                                leftover
                        )
                );

            } catch (RuntimeException exception) {

                logRecovery(
                        listingId,
                        buyer.getUniqueId(),
                        price,
                        "Payment succeeded but item delivery failed: "
                                + exception.getMessage()
                );

                buyer.sendMessage(
                        lang().get("auction.delivery-error")
                );

                return;
            }

            // Buyer notification
            buyer.sendMessage(
                    lang().get(
                            "auction.purchase-success",
                            "price",
                            economyManager.format(price)
                    )
            );

            // Seller notification
            Player onlineSeller = Bukkit.getPlayer(sellerUUID);

            if (onlineSeller != null && onlineSeller.isOnline()) {

                onlineSeller.sendMessage(
                        lang().get(
                                "auction.sold",
                                "player",
                                buyer.getName()
                        )
                );

                onlineSeller.sendMessage(
                        lang().get(
                                "auction.received-money",
                                "price",
                                economyManager.format(price)
                        )
                );
            }

            buyer.closeInventory();

        } catch (SQLException exception) {

            buyer.sendMessage(lang().get("auction.database-error"));

            plugin.getLogger().severe(
                    "Auction purchase error: "
                            + exception.getMessage()
            );

        } catch (RuntimeException exception) {

            buyer.sendMessage(lang().get("auction.general-error"));

            plugin.getLogger().severe(
                    "Auction runtime error: "
                            + exception.getMessage()
            );

        } finally {

            processing.remove(listingId);
        }
    }

    // ==========================================
    // REFUND
    // ==========================================

    private void refundBuyer(
            Player buyer,
            int listingId,
            double amount,
            String reason
    ) {

        boolean refunded = economyManager.deposit(buyer, amount);

        if (!refunded) {

            logRecovery(
                    listingId,
                    buyer.getUniqueId(),
                    amount,
                    reason + " Refund failed."
            );
        }
    }

    // ==========================================
    // RESTORE LISTING
    // ==========================================

    private boolean restoreListing(AuctionListing listing) {

        try {

            long remaining = listing.getExpiresAt()
                    - System.currentTimeMillis();

            if (remaining <= 0) {
                return false;
            }

            auctionManager.createListing(
                    listing.getSellerUUID(),
                    listing.getSellerName(),
                    listing.getItem(),
                    listing.getPrice(),
                    remaining
            );

            return true;

        } catch (Exception exception) {

            plugin.getLogger().severe(
                    "Failed to restore listing: "
                            + exception.getMessage()
            );

            return false;
        }
    }

    // ==========================================
    // RECOVERY LOG
    // ==========================================

    private void logRecovery(
            int listingId,
            UUID buyer,
            double amount,
            String reason
    ) {

        plugin.getLogger().severe(
                "AUCTION TRANSACTION REQUIRES ADMIN REVIEW! "
                        + "Listing=" + listingId
                        + ", Buyer=" + buyer
                        + ", Amount=" + amount
                        + ", Reason=" + reason
        );
    }
}
