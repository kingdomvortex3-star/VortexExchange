
package id.vortexexchange.auction;

import id.vortexexchange.VortexExchange;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;

public class AuctionCommand implements CommandExecutor {

    private final VortexExchange plugin;
    private final AuctionManager auctionManager;

    public AuctionCommand(
            VortexExchange plugin,
            AuctionManager auctionManager
    ) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Perintah ini hanya untuk pemain.");
            return true;
        }

        if (!player.hasPermission("vortexexchange.use")) {
            player.sendMessage(color("&cKamu tidak memiliki izin."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("auction.enabled", true)) {
            player.sendMessage(color("&cAuction House sedang dinonaktifkan."));
            return true;
        }

        if (args.length == 0) {
            openAuction(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            sellItem(player, args);
            return true;
        }

        player.sendMessage(color("&cGunakan: /ah atau /ah sell <harga>"));
        return true;
    }

    private void openAuction(Player player) {
        try {
            List<AuctionListing> listings =
                    auctionManager.getListings();

            AuctionGUI gui = new AuctionGUI(listings);
            player.openInventory(gui.getInventory());

        } catch (SQLException exception) {
            player.sendMessage(color("&cGagal membuka Auction House."));
            plugin.getLogger().severe(
                    "Gagal mengambil listing: " + exception.getMessage()
            );
        }
    }

    private void sellItem(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(color("&cGunakan: /ah sell <harga>"));
            return;
        }

        double price;

        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(color("&cHarga harus berupa angka."));
            return;
        }

        if (!Double.isFinite(price) || price <= 0) {
            player.sendMessage(color("&cHarga harus lebih dari 0."));
            return;
        }

        double minimum = plugin.getConfig().getDouble(
                "auction.minimum-price", 1
        );

        if (!Double.isFinite(minimum) || minimum < 0) {
            minimum = 1;
        }

        if (price < minimum) {
            player.sendMessage(color(
                    "&cHarga minimum: " + minimum
            ));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(color("&cPegang barang yang ingin dijual."));
            return;
        }

        int maximumListings = plugin.getConfig().getInt(
                "auction.max-listings-per-player", 10
        );

        if (maximumListings < 1) {
            maximumListings = 1;
        }

        try {
            if (!player.hasPermission("vortexexchange.admin")) {
                int currentListings = auctionManager.getPlayerListingCount(
                        player.getUniqueId()
                );

                if (currentListings >= maximumListings) {
                    player.sendMessage(color(
                            "&cKamu sudah mencapai batas maksimal "
                                    + maximumListings
                                    + " listing aktif!"
                    ));
                    return;
                }
            }

            long duration = plugin.getConfig().getLong(
                    "auction.listing-duration-hours", 48
            );

            if (duration <= 0
                    || duration > Long.MAX_VALUE / 3600000L) {
                player.sendMessage(color("&cDurasi listing tidak valid."));
                return;
            }

            ItemStack item = hand.clone();

            auctionManager.createListing(
                    player.getUniqueId(),
                    player.getName(),
                    item,
                    price,
                    duration * 3600000L
            );

            player.getInventory().setItemInMainHand(null);

            player.sendMessage(color(
                    "&aBarang berhasil dipasang di Auction House!"
            ));
            player.sendMessage(color("&7Harga: &b" + price));
            player.sendMessage(color(
                    "&7Listing aktif: &b"
                            + auctionManager.getPlayerListingCount(
                                    player.getUniqueId()
                            )
                            + "/" + maximumListings
            ));

        } catch (SQLException | RuntimeException exception) {
            player.sendMessage(color(
                    "&cGagal memasang barang. Barang kamu tidak diambil."
            ));

            plugin.getLogger().severe(
                    "Gagal membuat listing: " + exception.getMessage()
            );
        }
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
