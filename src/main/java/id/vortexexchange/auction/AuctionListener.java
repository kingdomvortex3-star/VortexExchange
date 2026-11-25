
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

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (event.getClick().isShiftClick()
                || event.getClick().isKeyboardClick()
                || event.getClick().isCreativeAction()) {
            return;
        }

        if (slot == 45) {
            changePage(player, gui, -1);
            return;
        }

        if (slot == 53) {
            changePage(player, gui, 1);
            return;
        }

        Integer listingId = gui.getListingId(slot);

        if (listingId == null) {
            return;
        }

        handleListingClick(player, listingId);
    }

    private void changePage(
            Player player,
            AuctionGUI currentGUI,
            int direction
    ) {

        if (player == null || !player.isOnline()) {
            return;
        }

        int newPage = currentGUI.getPage() + direction;

        if (newPage < 0
                || newPage >= currentGUI.getTotalPages()) {
            return;
        }

        try {

            var listings = auctionManager.getListings();

            AuctionGUI newGUI = new AuctionGUI(
                    listings,
                    newPage,
                    player.getUniqueId()
            );

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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory().getHolder()
                instanceof AuctionGUI) {

            event.setCancelled(true);
        }
    }

    private void handleListingClick(Player player, int listingId) {
        try {
            AuctionListing listing = auctionManager.getListing(listingId);

            if (listing == null || listing.isExpired()) {
                player.sendMessage(lang().get("auction.unavailable"));
                player.closeInventory();
                return;
            }

            if (listing.getSellerUUID().equals(player.getUniqueId())) {
                reclaimListing(player, listingId);
                return;
            }

            purchase(player, listingId);

        } catch (SQLException exception) {
            player.sendMessage(lang().get("auction.database-error"));
            plugin.getLogger().severe(
                    "Failed to inspect auction listing " + listingId
                            + ": " + exception.getMessage()
            );
        }
    }

    private void reclaimListing(Player seller, int listingId) {
        if (!processing.add(listingId)) {
            seller.sendMessage(lang().get("auction.processing"));
            return;
        }

        try {
            boolean reclaimed = auctionManager.cancelListingToMailbox(
                    listingId,
                    seller.getUniqueId()
            );

            if (!reclaimed) {
                seller.sendMessage(lang().get("auction.unavailable"));
                seller.closeInventory();
                return;
            }

            seller.closeInventory();
            seller.sendMessage(lang().get("auction.cancel-success"));

            plugin.getMailboxManager().deliver(seller);

            try {
                int remaining = plugin.getMailboxManager().getItemCount(
                        seller.getUniqueId()
                );

                if (remaining > 0) {
                    seller.sendMessage(lang().get(
                            "mailbox.remaining",
                            "count", String.valueOf(remaining)
                    ));
                    seller.sendMessage(lang().get("mailbox.empty-inventory"));
                }

            } catch (SQLException mailboxException) {
                plugin.getLogger().severe(
                        "Failed to check Mailbox after reclaim for "
                                + seller.getUniqueId() + ": "
                                + mailboxException.getMessage()
                );
                seller.sendMessage(lang().get("mailbox.error"));
            }

        } catch (SQLException exception) {
            seller.sendMessage(lang().get("auction.cancel-failed"));
            plugin.getLogger().severe(
                    "Failed to reclaim auction listing " + listingId
                            + " for " + seller.getUniqueId()
                            + ": " + exception.getMessage()
            );

        } finally {
            processing.remove(listingId);
        }
    }

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

            buyer.sendMessage(
                    lang().get(
                            "auction.purchase-success",
                            "price",
                            economyManager.format(price)
                    )
            );

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
