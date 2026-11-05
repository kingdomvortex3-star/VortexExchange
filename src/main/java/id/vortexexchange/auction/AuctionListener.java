
package id.vortexexchange.auction;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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

        if (event.getClickedInventory() == null) {
            return;
        }

        if (!event.getClickedInventory().equals(
                event.getView().getTopInventory()
        )) {
            return;
        }

        event.setCancelled(true);

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

    private synchronized void purchase(Player buyer, int listingId) {
        if (!processing.add(listingId)) {
            buyer.sendMessage(color("&cListing sedang diproses."));
            return;
        }

        try {
            if (!economyManager.isAvailable()) {
                buyer.sendMessage(color("&cEkonomi Vault tidak tersedia."));
                return;
            }

            AuctionListing listing =
                    auctionManager.getListing(listingId);

            if (listing == null || listing.isExpired()) {
                buyer.sendMessage(color("&cListing sudah tidak tersedia."));
                buyer.closeInventory();
                return;
            }

            if (listing.getSellerUUID().equals(buyer.getUniqueId())) {
                buyer.sendMessage(color("&cKamu tidak bisa membeli barang sendiri."));
                return;
            }

            if (!economyManager.has(buyer, listing.getPrice())) {
                buyer.sendMessage(color("&cSaldo kamu tidak cukup."));
                return;
            }

            Player seller = Bukkit.getPlayer(listing.getSellerUUID());

            if (!economyManager.withdraw(buyer, listing.getPrice())) {
                buyer.sendMessage(color("&cPembayaran gagal."));
                return;
            }

            if (!auctionManager.deleteListing(listingId)) {
                economyManager.deposit(buyer, listing.getPrice());
                buyer.sendMessage(color("&cListing sudah dibeli pemain lain."));
                return;
            }

            boolean deposited = economyManager.deposit(
                    Bukkit.getOfflinePlayer(listing.getSellerUUID()),
                    listing.getPrice()
            );

            if (!deposited) {
                boolean restored = false;

                try {
                    long remaining = listing.getExpiresAt()
                            - System.currentTimeMillis();

                    if (remaining > 0) {
                        auctionManager.createListing(
                                listing.getSellerUUID(),
                                listing.getSellerName(),
                                listing.getItem(),
                                listing.getPrice(),
                                remaining
                        );
                        restored = true;
                    }
                } catch (Exception exception) {
                    plugin.getLogger().severe(
                            "Gagal memulihkan listing " + listingId
                                    + ": " + exception.getMessage()
                    );
                }

                boolean refunded = economyManager.deposit(
                        buyer,
                        listing.getPrice()
                );

                if (!restored || !refunded) {
                    plugin.getLogger().severe(
                            "Perlu pemeriksaan transaksi listing "
                                    + listingId
                    );
                }

                buyer.sendMessage(color(
                        "&cTransaksi gagal. Hubungi admin jika saldo berubah."
                ));
                return;
            }

            ItemStack item = listing.getItem();

            var leftovers = buyer.getInventory().addItem(item);

            leftovers.values().forEach(leftover ->
                    buyer.getWorld().dropItemNaturally(
                            buyer.getLocation(),
                            leftover
                    )
            );

            buyer.sendMessage(color(
                    "&aBerhasil membeli barang seharga &e"
                            + economyManager.format(listing.getPrice())
            ));

            if (seller != null) {
                seller.sendMessage(color(
                        "&aBarang kamu dibeli oleh &f"
                                + buyer.getName()
                ));
                seller.sendMessage(color(
                        "&aKamu menerima &e"
                                + economyManager.format(listing.getPrice())
                ));
            }

            buyer.closeInventory();

        } catch (SQLException exception) {
            buyer.sendMessage(color("&cTerjadi kesalahan database."));
            plugin.getLogger().severe(
                    "Auction purchase error: " + exception.getMessage()
            );
        } finally {
            processing.remove(listingId);
        }
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
