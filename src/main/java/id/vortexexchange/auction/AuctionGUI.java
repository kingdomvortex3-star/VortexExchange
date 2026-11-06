
package id.vortexexchange.auction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Integer> listingSlots = new HashMap<>();

    private final int page;
    private final int totalPages;
    private final int totalListings;

    private static final int ITEMS_PER_PAGE = 28;

    private static final int[] LISTING_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final DecimalFormat PRICE_FORMAT =
            new DecimalFormat("#,##0.##");

    public AuctionGUI(List<AuctionListing> listings) {
        this(listings, 0);
    }

    public AuctionGUI(List<AuctionListing> listings, int requestedPage) {

        List<AuctionListing> availableListings = new ArrayList<>();

        if (listings != null) {
            for (AuctionListing listing : listings) {
                if (listing == null || listing.isExpired()) {
                    continue;
                }

                ItemStack item = listing.getItem();

                if (item == null || item.getType().isAir()) {
                    continue;
                }

                availableListings.add(listing);
            }
        }

        this.totalListings = availableListings.size();

        this.totalPages = Math.max(
                1,
                (int) Math.ceil(
                        (double) totalListings / ITEMS_PER_PAGE
                )
        );

        this.page = Math.max(
                0,
                Math.min(requestedPage, totalPages - 1)
        );

        inventory = Bukkit.createInventory(
                this,
                54,
                ChatColor.translateAlternateColorCodes(
                        '&',
                        "&0&lVortexExchange &8| &bAuction House"
                )
        );

        fillBackground();

        displayListings(availableListings);

        addControls();
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

    private void displayListings(
            List<AuctionListing> listings
    ) {

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

        int startIndex = page * ITEMS_PER_PAGE;

        int endIndex = Math.min(
                startIndex + ITEMS_PER_PAGE,
                listings.size()
        );

        int displayIndex = 0;

        for (int i = startIndex; i < endIndex; i++) {

            AuctionListing listing = listings.get(i);

            ItemStack display = listing.getItem().clone();

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

            int slot = LISTING_SLOTS[displayIndex];

            inventory.setItem(slot, display);

            listingSlots.put(slot, listing.getId());

            displayIndex++;
        }
    }

    private void addControls() {

        inventory.setItem(
                4,
                createItem(
                        Material.NETHER_STAR,
                        "&b&lVORTEX AUCTION HOUSE",
                        "&7Tempat jual beli antar pemain.",
                        "",
                        "&7Listing tersedia: &b" + totalListings,
                        "&7Halaman: &b" + (page + 1)
                                + "&7/&b" + totalPages,
                        "&8&m--------------------",
                        "&ePilih barang untuk membeli."
                )
        );

        if (page > 0) {

            inventory.setItem(
                    45,
                    createItem(
                            Material.ARROW,
                            "&e&lHalaman sebelumnya",
                            "&7Kembali ke halaman &b" + page,
                            "",
                            "&eKlik untuk kembali."
                    )
            );

        } else {

            inventory.setItem(
                    45,
                    createItem(
                            Material.GRAY_STAINED_GLASS_PANE,
                            "&8Halaman pertama",
                            "&7Tidak ada halaman sebelumnya."
                    )
            );
        }

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "&cTutup Auction House",
                        "&7Klik untuk menutup."
                )
        );

        if (page < totalPages - 1) {

            inventory.setItem(
                    53,
                    createItem(
                            Material.ARROW,
                            "&e&lHalaman berikutnya",
                            "&7Lanjut ke halaman &b" + (page + 2),
                            "",
                            "&eKlik untuk melanjutkan."
                    )
            );

        } else {

            inventory.setItem(
                    53,
                    createItem(
                            Material.GRAY_STAINED_GLASS_PANE,
                            "&8Halaman terakhir",
                            "&7Tidak ada halaman berikutnya."
                    )
            );
        }
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

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
