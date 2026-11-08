
package id.vortexexchange.auction;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class AuctionGUI implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Integer> listingSlots = new HashMap<>();

    private final int page;
    private final int totalPages;
    private final int totalListings;

    private static final int ITEMS_PER_PAGE = 51;

    private static final int[] LISTING_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            46, 47, 48,
            50, 51, 52
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

        this.inventory = Bukkit.createInventory(
                this,
                54,
                color("&0&lVORTEX &8» &bAuction House")
        );

        displayListings(availableListings);
        addControls();
    }

    private void displayListings(List<AuctionListing> listings) {

        if (listings == null || listings.isEmpty()) {

            inventory.setItem(
                    22,
                    createItem(
                            Material.BARRIER,
                            "&c&lMARKETPLACE KOSONG",
                            "",
                            "&7Belum ada barang yang dijual.",
                            "&7Silakan cek kembali nanti."
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

                List<String> lore = new ArrayList<>();

                lore.add(color("&8&m--------------------------"));
                lore.add(color("&b&lSELLER INFORMATION"));
                lore.add(color("&7Penjual: &f" + listing.getSellerName()));
                lore.add(color(""));
                lore.add(color("&a&lPRICE"));
                lore.add(color("&f$" + PRICE_FORMAT.format(
                        listing.getPrice()
                )));
                lore.add(color(""));
                lore.add(color("&8Listing ID: &7#" + listing.getId()));
                lore.add(color("&8&m--------------------------"));
                lore.add(color("&e&lKLIK UNTUK MEMBELI"));

                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                display.setItemMeta(meta);
            }

            int slot = LISTING_SLOTS[displayIndex];

            inventory.setItem(slot, display);
            listingSlots.put(slot, listing.getId());

            displayIndex++;
        }
    }

    private void addControls() {

        // Previous page - bottom left
        if (page > 0) {

            inventory.setItem(
                    45,
                    createItem(
                            Material.BOOK,
                            "&b&lPREVIOUS PAGE",
                            "",
                            "&7Halaman sebelumnya: &f" + page,
                            "",
                            "&eKlik untuk kembali."
                    )
            );

        } else {

            inventory.setItem(
                    45,
                    createItem(
                            Material.PAPER,
                            "&8&lHALAMAN PERTAMA",
                            "",
                            "&7Tidak ada halaman sebelumnya."
                    )
            );
        }

        // Close - bottom center
        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "&c&lCLOSE AUCTION HOUSE",
                        "",
                        "&7Klik untuk menutup menu."
                )
        );

        // Next page - bottom right
        if (page < totalPages - 1) {

            inventory.setItem(
                    53,
                    createItem(
                            Material.BOOK,
                            "&b&lNEXT PAGE",
                            "",
                            "&7Halaman berikutnya: &f" + (page + 2),
                            "",
                            "&eKlik untuk melanjutkan."
                    )
            );

        } else {

            inventory.setItem(
                    53,
                    createItem(
                            Material.PAPER,
                            "&8&lHALAMAN TERAKHIR",
                            "",
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
                        Arrays.stream(lore)
                                .map(this::color)
                                .toList()
                );
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

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

    public int getTotalListings() {
        return totalListings;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
