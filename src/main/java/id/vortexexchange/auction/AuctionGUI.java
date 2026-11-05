
package id.vortexexchange.auction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Integer> listingSlots = new HashMap<>();

    public AuctionGUI(List<AuctionListing> listings) {
        inventory = Bukkit.createInventory(
                this,
                54,
                ChatColor.translateAlternateColorCodes(
                        '&',
                        "&0&lVortexExchange &8| &bAuction"
                )
        );

        fillBackground();
        displayListings(listings);
        addControls();
    }

    private void fillBackground() {
        ItemStack glass = new ItemStack(
                Material.GRAY_STAINED_GLASS_PANE
        );

        ItemMeta meta = glass.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }

        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45
                    || slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, glass);
            }
        }
    }

    private void displayListings(List<AuctionListing> listings) {
        if (listings == null) {
            return;
        }

        int slot = 10;

        for (AuctionListing listing : listings) {
            if (listing == null || listing.isExpired()) {
                continue;
            }

            if (slot >= 44) {
                break;
            }

            ItemStack display = listing.getItem();
            ItemMeta meta = display.getItemMeta();

            if (meta != null) {
                meta.setLore(List.of(
                        ChatColor.GRAY + "Penjual: "
                                + ChatColor.WHITE + listing.getSellerName(),
                        ChatColor.GRAY + "Harga: "
                                + ChatColor.GREEN + listing.getPrice(),
                        ChatColor.YELLOW + "Klik untuk membeli"
                ));

                display.setItemMeta(meta);
            }

            inventory.setItem(slot, display);
            listingSlots.put(slot, listing.getId());

            slot++;

            if (slot % 9 == 8) {
                slot += 2;
            }
        }
    }

    private void addControls() {
        inventory.setItem(49, createItem(
                Material.BARRIER,
                "&cTutup"
        ));

        inventory.setItem(45, createItem(
                Material.ARROW,
                "&eHalaman sebelumnya"
        ));

        inventory.setItem(53, createItem(
                Material.ARROW,
                "&eHalaman berikutnya"
        ));

        inventory.setItem(4, createItem(
                Material.NETHER_STAR,
                "&b&lVortex Auction House"
        ));
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(
                    ChatColor.translateAlternateColorCodes('&', name)
            );
            item.setItemMeta(meta);
        }

        return item;
    }

    public Integer getListingId(int slot) {
        return listingSlots.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
