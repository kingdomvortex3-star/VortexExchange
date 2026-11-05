
package id.vortexexchange.auction;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
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

        Integer listingId = gui.getListingId(slot);

        if (listingId == null) {
            return;
        }

        if (event.getClick().isShiftClick()
                || event.getClick().isKeyboardClick()
                || event.getClick().isCreativeAction()) {
            return;
        }

        purchase(player, listingId);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof AuctionGUI) {
            event.setCancelled(true);
        }
    }

    private void purchase(Player buyer, int listingId) {
        if (!processing.add(listingId)) {
            buyer.sendMessage(color("&cListing sedang diproses."));
            return;
        }

        try {
            if (!plugin.getConfig().getBoolean(
                    "auction.enabled", true)) {
                buyer.sendMessage(color(
                        "&cAuction House sedang dinonaktifkan."
                ));
                return;
            }

            if (!economyManager.isAvailable()) {
                buyer.sendMessage(color(
                        "&cEkonomi Vault tidak tersedia."
                ));
                return;
            }

            AuctionListing listing =
                    auctionManager.getListing(listingId);

            if (listing == null || listing.isExpired()) {
                buyer.sendMessage(color(
                        "&cListing sudah tidak tersedia."
                ));
                buyer.closeInventory();
                return;
            }

            UUID sellerUUID = listing.getSellerUUID();

            if (sellerUUID == null) {
                buyer.sendMessage(color("&cData penjual tidak valid."));
                return;
            }

            if (sellerUUID.equals(buyer.getUniqueId())) {
                buyer.sendMessage(color(
                        "&cKamu tidak bisa membeli barang sendiri."
                ));
                return;
            }

            ItemStack item = listing.getItem();

            if (item == null || item.getType().isAir()
                    || item.getAmount() <= 0) {
                buyer.sendMessage(color(
                        "&cData barang tidak valid. Hubungi admin."
                ));

                plugin.getLogger().severe(
                        "Item listing tidak valid. ID: " + listingId
                );
                return;
            }

            double price = listing.getPrice();

            if (!Double.isFinite(price) || price <= 0) {
                buyer.sendMessage(color("&cHarga listing tidak valid."));
                return;
            }

            if (!economyManager.has(buyer, price)) {
                buyer.sendMessage(color("&cSaldo kamu tidak cukup."));
                return;
            }

            OfflinePlayer seller =
                    Bukkit.getOfflinePlayer(sellerUUID);

            if (!economyManager.withdraw(buyer, price)) {
                buyer.sendMessage(color("&cPembayaran gagal."));
                return;
            }

            boolean deleted;

            try {
                deleted = auctionManager.deleteListing(listingId);
            } catch (SQLException exception) {
                refundBuyer(buyer, listingId, price,
                        "Gagal menghapus listing: "
                                + exception.getMessage());

                buyer.sendMessage(color(
                        "&cTransaksi gagal. Periksa saldo kamu."
                ));
                return;
            }

            if (!deleted) {
                refundBuyer(
                        buyer,
                        listingId,
                        price,
                        "Listing tidak berhasil dihapus."
                );

                buyer.sendMessage(color(
                        "&cListing sudah tidak tersedia."
                ));
                return;
            }

            if (!economyManager.deposit(seller, price)) {
                boolean restored = restoreListing(listing);

                boolean refunded = economyManager.deposit(buyer, price);

                logRecovery(
                        listingId,
                        buyer.getUniqueId(),
                        price,
                        "Pembayaran seller gagal. Listing restored="
                                + restored
                                + ", buyer refunded=" + refunded
                );

                buyer.sendMessage(color(
                        "&cTransaksi bermasalah. Hubungi admin."
                ));
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
                        "Pembayaran berhasil tetapi pengiriman item gagal: "
                                + exception.getMessage()
                );

                buyer.sendMessage(color(
                        "&cPembayaran berhasil, tetapi pengiriman barang "
                                + "bermasalah. Hubungi admin."
                ));

                return;
            }

            buyer.sendMessage(color(
                    "&aBerhasil membeli barang seharga &e"
                            + economyManager.format(price)
            ));

            Player onlineSeller = Bukkit.getPlayer(sellerUUID);

            if (onlineSeller != null && onlineSeller.isOnline()) {
                onlineSeller.sendMessage(color(
                        "&aBarang kamu dibeli oleh &f" + buyer.getName()
                ));

                onlineSeller.sendMessage(color(
                        "&aKamu menerima &e"
                                + economyManager.format(price)
                ));
            }

            buyer.closeInventory();

        } catch (SQLException exception) {
            buyer.sendMessage(color(
                    "&cTerjadi kesalahan database."
            ));

            plugin.getLogger().severe(
                    "Auction purchase error: " + exception.getMessage()
            );

        } catch (RuntimeException exception) {
            buyer.sendMessage(color(
                    "&cTerjadi kesalahan saat membeli barang. Hubungi admin."
            ));

            plugin.getLogger().severe(
                    "Auction runtime error: " + exception.getMessage()
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
                    reason + " Refund gagal."
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
                    "Gagal memulihkan listing: "
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
                "AUCTION TRANSACTION MEMERLUKAN PEMERIKSAAN ADMIN! "
                        + "Listing=" + listingId
                        + ", Buyer=" + buyer
                        + ", Amount=" + amount
                        + ", Reason=" + reason
        );
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
