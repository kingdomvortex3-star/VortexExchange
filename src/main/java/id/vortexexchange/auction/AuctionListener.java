
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
            if (!plugin.getConfig().getBoolean("auction.enabled", true)) {
                buyer.sendMessage(color("&cAuction House sedang dinonaktifkan."));
                return;
            }

            if (!economyManager.isAvailable()) {
                buyer.sendMessage(color("&cEkonomi Vault tidak tersedia."));
                return;
            }

            AuctionListing listing = auctionManager.getListing(listingId);

            if (listing == null || listing.isExpired()) {
                buyer.sendMessage(color("&cListing sudah tidak tersedia."));
                buyer.closeInventory();
                return;
            }

            if (listing.getSellerUUID().equals(buyer.getUniqueId())) {
                buyer.sendMessage(color("&cKamu tidak bisa membeli barang sendiri."));
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

            OfflinePlayer seller = Bukkit.getOfflinePlayer(
                    listing.getSellerUUID()
            );

            if (!economyManager.withdraw(buyer, price)) {
                buyer.sendMessage(color("&cPembayaran gagal."));
                return;
            }

            boolean deleted;

            try {
                deleted = auctionManager.deleteListing(listingId);
            } catch (SQLException exception) {
                boolean refunded = economyManager.deposit(buyer, price);

                if (!refunded) {
                    logRecovery(
                            listingId,
                            buyer.getUniqueId(),
                            price,
                            "Database error dan refund gagal"
                    );
                }

                buyer.sendMessage(color(
                        "&cTransaksi gagal. Hubungi admin jika saldo berubah."
                ));

                plugin.getLogger().severe(
                        "Gagal menghapus listing " + listingId
                                + ": " + exception.getMessage()
                );

                return;
            }

            if (!deleted) {
                boolean refunded = economyManager.deposit(buyer, price);

                if (!refunded) {
                    logRecovery(
                            listingId,
                            buyer.getUniqueId(),
                            price,
                            "Listing gagal dihapus dan refund gagal"
                    );
                }

                buyer.sendMessage(color(
                        "&cListing sudah tidak tersedia. Saldo sedang diperiksa."
                ));

                return;
            }

            boolean deposited = economyManager.deposit(seller, price);

            if (!deposited) {
                boolean restored = restoreListing(listing);

                boolean refunded = economyManager.deposit(buyer, price);

                logRecovery(
                        listingId,
                        buyer.getUniqueId(),
                        price,
                        "Pembayaran seller gagal. Listing restored="
                                + restored + ", buyer refunded=" + refunded
                );

                buyer.sendMessage(color(
                        "&cPembayaran gagal. Hubungi admin jika saldo berubah."
                ));

                return;
            }

            ItemStack item = listing.getItem();

            if (item == null || item.getType().isAir()) {
                plugin.getLogger().severe(
                        "Listing " + listingId + " memiliki item tidak valid."
                );

                logRecovery(
                        listingId,
                        buyer.getUniqueId(),
                        price,
                        "Item listing tidak valid setelah pembayaran"
                );

                buyer.sendMessage(color(
                        "&cTerjadi kesalahan item. Hubungi admin."
                ));

                return;
            }

            var leftovers = buyer.getInventory().addItem(item.clone());

            leftovers.values().forEach(leftover ->
                    buyer.getWorld().dropItemNaturally(
                            buyer.getLocation(),
                            leftover
                    )
            );

            buyer.sendMessage(color(
                    "&aBerhasil membeli barang seharga &e"
                            + economyManager.format(price)
            ));

            Player onlineSeller = Bukkit.getPlayer(
                    listing.getSellerUUID()
            );

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
            buyer.sendMessage(color("&cTerjadi kesalahan database."));

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
            java.util.UUID buyer,
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
