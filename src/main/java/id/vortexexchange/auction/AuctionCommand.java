
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

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang().get("player.only"));
            return true;
        }

        if (!player.hasPermission("vortexexchange.use")) {
            player.sendMessage(lang().get("permission.denied"));
            return true;
        }

        if (!plugin.getConfig().getBoolean("auction.enabled", true)) {
            player.sendMessage(lang().get("auction.disabled"));
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

        player.sendMessage(lang().get("auction.usage"));
        return true;
    }

    private void openAuction(Player player) {
        try {
            List<AuctionListing> listings = auctionManager.getListings();

            AuctionGUI gui = new AuctionGUI(listings);
            player.openInventory(gui.getInventory());

        } catch (SQLException exception) {
            player.sendMessage(lang().get("auction.database-error"));

            plugin.getLogger().severe(
                    "Failed to load auction listings: "
                            + exception.getMessage()
            );
        }
    }

    private void sellItem(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(lang().get("auction.usage"));
            return;
        }

        double price;

        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(lang().get("auction.invalid-price"));
            return;
        }

        if (!Double.isFinite(price) || price <= 0) {
            player.sendMessage(lang().get("auction.invalid-price"));
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
                    lang().get(
                            "auction.minimum",
                            "price",
                            String.valueOf(minimum)
                    )
            );
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(lang().get("auction.hold-item"));
            return;
        }

        int maximumListings = plugin.getConfig().getInt(
                "auction.max-listings-per-player", 10
        );

        maximumListings = Math.max(1, maximumListings);

        try {
            if (!player.hasPermission("vortexexchange.admin")) {
                int currentListings = auctionManager.getPlayerListingCount(
                        player.getUniqueId()
                );

                if (currentListings >= maximumListings) {
                    player.sendMessage(
                            lang().get(
                                    "auction.limit",
                                    "limit",
                                    String.valueOf(maximumListings)
                            )
                    );
                    return;
                }
            }

            long duration = plugin.getConfig().getLong(
                    "auction.listing-duration-hours", 168
            );

            if (duration <= 0 || duration > Long.MAX_VALUE / 3600000L) {
                player.sendMessage(lang().get("auction.duration-invalid"));
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

            player.sendMessage(lang().get("auction.success"));
            player.sendMessage(
                    lang().get(
                            "auction.price",
                            "price",
                            String.valueOf(price)
                    )
            );

            int activeListings = auctionManager.getPlayerListingCount(
                    player.getUniqueId()
            );

            player.sendMessage(
                    lang().get(
                            "auction.listing-count",
                            "current",
                            String.valueOf(activeListings),
                            "maximum",
                            String.valueOf(maximumListings)
                    )
            );

            broadcastListing(player, item, price);

        } catch (SQLException | RuntimeException exception) {
            player.sendMessage(lang().get("auction.failed"));

            plugin.getLogger().severe(
                    "Failed to create auction listing: "
                            + exception.getMessage()
            );
        }
    }

    private void broadcastListing(
            Player seller,
            ItemStack item,
            double price
    ) {
        String itemName = item.getType().name()
                .toLowerCase()
                .replace('_', ' ');

        String announcement =
                "&8&m--------------------------------\n"
                + "&b&lVortexExchange &8» "
                + lang().get("auction.new") + "\n"
                + lang().get(
                        "auction.seller",
                        "player",
                        seller.getName()
                ) + "\n"
                + lang().get(
                        "auction.item",
                        "amount",
                        String.valueOf(item.getAmount()),
                        "item",
                        itemName
                ) + "\n"
                + lang().get(
                        "auction.price",
                        "price",
                        String.valueOf(price)
                ) + "\n"
                + lang().get("auction.view") + "\n"
                + "&8&m--------------------------------";

        Bukkit.broadcastMessage(color(announcement));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
