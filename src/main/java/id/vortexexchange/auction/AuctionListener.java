
package id.vortexexchange.auction;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
import id.vortexexchange.util.LanguageManager;

import org.bukkit.Bukkit;
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

        if (event.getClick().isShiftClick()
                || event.getClick().isKeyboardClick()
                || event.getClick().isCreativeAction()) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getView().getTopInventory().getHolder()
                instanceof AuctionGUI) {
            event.setCancelled(true);
        }
    }

    private void changePage(
            Player player,
            AuctionGUI currentGUI,
            int direction
    ) {

        int newPage = currentGUI.getPage() + direction;

        if (newPage < 0
                || newPage >= currentGUI.getTotalPages()) {
            return;
        }

        try {

            AuctionGUI newGUI = new AuctionGUI(
                    auctionManager.getListings(),
                    newPage,
                    player.getUniqueId()
            );

            player.openInventory(newGUI.getInventory());

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Failed to load auction page: "
                            + exception.getMessage()
            );

            player.sendMessage(
                    lang().get("auction.page-error")
            );
        }
    }

    private void handleListingClick(
            Player player,
            int listingId
    ) {

        if (!processing.add(listingId)) {
            player.sendMessage(lang().get("auction.processing"));
            return;
        }

        try {

            AuctionListing listing =
                    auctionManager.getListing(listingId);

            if (listing == null || listing.isExpired()) {
                player.sendMessage(lang().get("auction.unavailable"));
                player.closeInventory();
                return;
            }

            if (listing.getSellerUUID().equals(player.getUniqueId())) {

                reclaimListing(player, listingId);
                return;
            }

            purchase(player, listing);

        } catch (SQLException exception) {

            player.sendMessage(lang().get("auction.database-error"));

            plugin.getLogger().severe(
                    "Failed to load listing " + listingId
                            + ": " + exception.getMessage()
            );

        } finally {
            processing.remove(listingId);
        }
    }

    private void reclaimListing(
            Player player,
            int listingId
    ) {

        try {

            boolean success =
                    auctionManager.cancelListingToMailbox(
                            listingId,
                            player.getUniqueId()
                    );

            if (!success) {
                player.sendMessage(
                        lang().get("auction.unavailable")
                );
                return;
            }

            player.closeInventory();

            player.sendMessage(
                    lang().get("auction.cancel-title")
            );

            player.sendMessage(
                    lang().get("auction.cancel-success")
            );

            player.sendMessage(
                    lang().get("auction.cancel-detail")
            );

            plugin.getMailboxManager().deliver(player);

        } catch (SQLException exception) {

            player.sendMessage(
                    lang().get("auction.cancel-failed")
            );

            plugin.getLogger().severe(
                    "Failed to reclaim listing " + listingId
                            + " for " + player.getUniqueId()
                            + ": " + exception.getMessage()
            );
        }
    }

    private void purchase(
            Player buyer,
            AuctionListing listing
    ) {

        int listingId = listing.getId();

        if (!plugin.getConfig().getBoolean(
                "auction.enabled", true)) {
            buyer.sendMessage(lang().get("auction.disabled"));
            return;
        }

        if (!economyManager.isAvailable()) {
            buyer.sendMessage(lang().get("economy.missing"));
            return;
        }

        UUID sellerUUID = listing.getSellerUUID();

        if (sellerUUID.equals(buyer.getUniqueId())) {
            buyer.sendMessage(lang().get("auction.own-item"));
            return;
        }

        ItemStack item = listing.getItem();

        if (item == null || item.getType().isAir()) {
            buyer.sendMessage(lang().get("auction.invalid-item"));
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

        OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerUUID);

        if (!economyManager.withdraw(buyer, price)) {
            buyer.sendMessage(lang().get("auction.payment-failed"));
            return;
        }

        boolean deleted;

        try {

            deleted = auctionManager.deleteListing(listingId);

        } catch (SQLException exception) {

            boolean refunded = economyManager.deposit(buyer, price);

            plugin.getLogger().severe(
                    "Auction delete failed. Listing=" + listingId
                            + ", Refunded=" + refunded
                            + ": " + exception.getMessage()
            );

            buyer.sendMessage(
                    lang().get("auction.transaction-error")
            );

            return;
        }

        if (!deleted) {

            economyManager.deposit(buyer, price);

            buyer.sendMessage(lang().get("auction.unavailable"));
            return;
        }

        if (!economyManager.deposit(seller, price)) {

            boolean restored = restoreListing(listing);
            boolean refunded = economyManager.deposit(buyer, price);

            plugin.getLogger().severe(
                    "Auction recovery required. Listing=" + listingId
                            + ", Restored=" + restored
                            + ", Refunded=" + refunded
            );

            buyer.sendMessage(
                    lang().get("auction.transaction-error")
            );

            return;
        }

        try {

            var leftovers = buyer.getInventory().addItem(item.clone());

            leftovers.values().forEach(leftover ->
                    buyer.getWorld().dropItemNaturally(
                            buyer.getLocation(),
                            leftover
                    )
            );

        } catch (RuntimeException exception) {

            plugin.getLogger().severe(
                    "Item delivery failed for listing "
                            + listingId + ": " + exception.getMessage()
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

        if (onlineSeller != null) {

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
}
