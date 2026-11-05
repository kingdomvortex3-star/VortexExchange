
package id.vortexexchange.auction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Integer> listingSlots = new HashMap<>();

    private static final int[] LISTING_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final DecimalFormat PRICE_FORMAT =
            new DecimalFormat("#,##0.##");

    public AuctionGUI(List<AuctionListing> listings) {
        inventory = Bukkit.createInventory(
                this,
                54,
                ChatColor.translateAlternateColorCodes(
                        '&',
                        "&0&lVortexExchange &8| &bAuction House"
                )
        );

        fillBackground();
        displayListings(listings);
        addControls(listings == null ? 0 : listings.size());
    }

    private void fillBackground() {
        ItemStack glass = createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                "&8"
        );

        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9
                    || slot >= 45
                    || slot % 9 == 0
                    || slot % 9 == 8) {
                inventory.setItem(slot, glass);
            }
        }
    }

    private void displayListings(List<AuctionListing> listings) {
        if (listings == null || listings.isEmpty()) {
            inventory.setItem(
                    22,
                    createItem(
                            Material.BARRIER,
                            "&cBelum ada barang dijual",
                            "&7Auction House masih kosong."
                    )
            );
            return;
        }

        int index = 0;

        for (AuctionListing listing : listings) {
            if (listing == null || listing.isExpired()) {
                continue;
            }

            if (index >= LISTING_SLOTS.length) {
                break;
            }

            ItemStack display = listing.getItem();

            if (display == null || display.getType().isAir()) {
                continue;
            }

            display = display.clone();

            ItemMeta meta = display.getItemMeta();

            if (meta != null) {
                meta.setLore(List.of(
                        color("&8&m--------------------"),
                        color("&7Penjual: &f" + listing.getSellerName()),
                        color("&7Harga: &a$" + PRICE_FORMAT.format(
                                listing.getPrice()
                        )),
                        color("&7Listing ID: &b#" + listing.getId()),
                        color("&8&m--------------------"),
                        color("&eKlik untuk membeli")
                ));

                display.setItemMeta(meta);
            }

            int slot = LISTING_SLOTS[index];

            inventory.setItem(slot, display);
            listingSlots.put(slot, listing.getId());

            index++;
        }

        if (index == 0) {
            inventory.setItem(
                    22,
                    createItem(
                            Material.BARRIER,
                            "&cBelum ada barang dijual",
                            "&7Auction House masih kosong."
                    )
            );
        }
    }

    private void addControls(int totalListings) {
        inventory.setItem(
                4,
                createItem(
                        Material.NETHER_STAR,
                        "&b&lVORTEX AUCTION HOUSE",
                        "&7Tempat jual beli antar pemain.",
                        "",
                        "&7Listing tersedia: &b" + totalListings,
                        "&8&m--------------------",
                        "&ePilih barang untuk membeli."
                )
        );

        inventory.setItem(
                45,
                createItem(
                        Material.ARROW,
                        "&eHalaman sebelumnya",
                        "&7Belum tersedia."
                )
        );

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "&cTutup Auction House",
                        "&7Klik untuk menutup."
                )
        );

        inventory.setItem(
                53,
                createItem(
                        Material.ARROW,
                        "&eHalaman berikutnya",
                        "&7Belum tersedia."
                )
        );
    }

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            if (lore != null && lore.length > 0) {
                meta.setLore(
                        java.util.Arrays.stream(lore)
                                .map(this::color)
                                .toList()
                );
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public Integer getListingId(int slot) {
        return listingSlots.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
