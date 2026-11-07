
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
                (int) Math.ceil((double) totalListings / ITEMS_PER_PAGE)
        );

        this.page = Math.max(
                0,
                Math.min(requestedPage, totalPages - 1)
        );

        inventory = Bukkit.createInventory(
                this,
                54,
                color("&0&lVORTEX &8» &bAuction House")
        );

        fillBackground();
        addHeader();
        displayListings(availableListings);
        addControls();
    }

    private void fillBackground() {

        ItemStack dark = createItem(
                Material.BLACK_STAINED_GLASS_PANE,
                "&0"
        );

        ItemStack blue = createItem(
                Material.BLUE_STAINED_GLASS_PANE,
                "&1"
        );

        ItemStack cyan = createItem(
                Material.CYAN_STAINED_GLASS_PANE,
                "&b"
        );

        for (int slot = 0; slot < 54; slot++) {

            if (slot < 9 || slot >= 45) {
                inventory.setItem(slot, dark);
            }

            if (slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, blue);
            }
        }

        inventory.setItem(0, cyan);
        inventory.setItem(8, cyan);
        inventory.setItem(45, blue);
        inventory.setItem(49, cyan);
        inventory.setItem(53, blue);
    }

    private void addHeader() {

        inventory.setItem(
                4,
                createItem(
                        Material.NETHER_STAR,
                        "&b&lVORTEXEXCHANGE",
                        "&8Auction House &7• &bMarketplace",
                        "",
                        "&7Tempat jual beli antar pemain.",
                        "&7Transaksi cepat dan mudah.",
                        "",
                        "&8&m---------------------------",
                        "&fTotal listing &8» &b" + totalListings,
                        "&fHalaman &8» &b" + (page + 1)
                                + "&7/&b" + totalPages,
                        "&8&m---------------------------",
                        "",
                        "&ePilih item untuk melihat pembelian."
                )
        );
    }

    private void displayListings(List<AuctionListing> listings) {

        if (listings == null || listings.isEmpty()) {

            inventory.setItem(
                    22,
                    createItem(
                            Material.BARRIER,
                            "&c&lMARKETPLACE KOSONG",
                            "",
                            "&7Belum ada pemain yang menjual item.",
                            "&7Silakan cek kembali nanti.",
                            "",
                            "&8VortexExchange"
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
                lore.add(color("&7Penjual &8» &f" + listing.getSellerName()));
                lore.add(color(""));
                lore.add(color("&a&lPRICE"));
                lore.add(color("&f$" + PRICE_FORMAT.format(
                        listing.getPrice()
                )));
                lore.add(color(""));
                lore.add(color("&8Listing ID: &7#" + listing.getId()));
                lore.add(color("&8&m--------------------------"));
                lore.add(color("&e&lLEFT CLICK"));
                lore.add(color("&7Klik untuk membeli item ini."));

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

        if (page > 0) {

            inventory.setItem(
                    45,
                    createItem(
                            Material.ARROW,
                            "&b&lPREVIOUS PAGE",
                            "",
                            "&7Kembali ke halaman &f" + page,
                            "",
                            "&eKlik untuk kembali."
                    )
            );

        } else {

            inventory.setItem(
                    45,
                    createItem(
                            Material.GRAY_STAINED_GLASS_PANE,
                            "&8FIRST PAGE",
                            "&7Kamu berada di halaman pertama."
                    )
            );
        }

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "&c&lCLOSE MENU",
                        "",
                        "&7Tutup Auction House.",
                        "",
                        "&cKlik untuk keluar."
                )
        );

        if (page < totalPages - 1) {

            inventory.setItem(
                    53,
                    createItem(
                            Material.ARROW,
                            "&b&lNEXT PAGE",
                            "",
                            "&7Lanjut ke halaman &f" + (page + 2),
                            "",
                            "&eKlik untuk melanjutkan."
                    )
            );

        } else {

            inventory.setItem(
                    53,
                    createItem(
                            Material.GRAY_STAINED_GLASS_PANE,
                            "&8LAST PAGE",
                            "&7Kamu berada di halaman terakhir."
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
