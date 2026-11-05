
package id.vortexexchange.auction;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.util.LanguageManager;
import org.bukkit.Bukkit;
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
        LanguageManager lang = plugin.getLanguageManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("vortexexchange.use")) {
            player.sendMessage(lang.get("permission.denied"));
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
        LanguageManager lang = plugin.getLanguageManager();

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
            player.sendMessage(
                    lang.get("auction.minimum", "price",
                            String.valueOf(minimum))
            );
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
                    player.sendMessage(
                            lang.get("auction.limit")
                                    + color(" &7(" + maximumListings + ")")
                    );
                    return;
                }
            }

            long duration = plugin.getConfig().getLong(
                    "auction.listing-duration-hours", 168
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

            player.sendMessage(lang.get("auction.success"));
            player.sendMessage(
                    lang.get("auction.price", "price", String.valueOf(price))
            );

            int activeListings = auctionManager.getPlayerListingCount(
                    player.getUniqueId()
            );

            player.sendMessage(color(
                    "&7Listing aktif: &b"
                            + activeListings
                            + "/" + maximumListings
            ));

            broadcastListing(player, item, price);

        } catch (SQLException | RuntimeException exception) {
            player.sendMessage(lang.get("auction.failed"));

            plugin.getLogger().severe(
                    "Gagal membuat listing: " + exception.getMessage()
            );
        }
    }

    private void broadcastListing(
            Player seller,
            ItemStack item,
            double price
    ) {
        LanguageManager lang = plugin.getLanguageManager();

        String itemName = item.getType().name()
                .toLowerCase()
                .replace('_', ' ');

        String announcement =
                "&8&m--------------------------------\n"
                + "&b&lVortexExchange &8» "
                + lang.get("auction.new") + "\n"
                + lang.get(
                        "auction.seller",
                        "player",
                        seller.getName()
                ) + "\n"
                + lang.get(
                        "auction.item",
                        "amount",
                        String.valueOf(item.getAmount()),
                        "item",
                        itemName
                ) + "\n"
                + lang.get(
                        "auction.price",
                        "price",
                        String.valueOf(price)
                ) + "\n"
                + lang.get("auction.view") + "\n"
                + "&8&m--------------------------------";

        Bukkit.broadcastMessage(color(announcement));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
