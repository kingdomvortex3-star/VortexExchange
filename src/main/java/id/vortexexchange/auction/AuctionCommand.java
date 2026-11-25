
package id.vortexexchange.auction;

import id.vortexexchange.util.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionGUI {

    private static final int SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    private final Inventory inventory;
    private final List<AuctionListing> listings;
    private final int page;
    private final UUID viewerUUID;

    public AuctionGUI(List<AuctionListing> listings) {
        this(listings, 0, null);
    }

    public AuctionGUI(List<AuctionListing> listings, int requestedPage) {
        this(listings, requestedPage, null);
    }

    public AuctionGUI(
            List<AuctionListing> listings,
            int requestedPage,
            UUID viewerUUID
    ) {
        this.listings = listings;
        this.viewerUUID = viewerUUID;

        int maxPage = Math.max(
                0,
                (listings.size() - 1) / ITEMS_PER_PAGE
        );

        this.page = Math.max(0, Math.min(requestedPage, maxPage));

        this.inventory = Bukkit.createInventory(
                null,
                SIZE,
                "§8§lVortex Auction House §7(" + (page + 1) + ")"
        );

        render();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getPage() {
        return page;
    }

    public List<AuctionListing> getListings() {
        return listings;
    }

    private void render() {
        fillBackground();

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(
                start + ITEMS_PER_PAGE,
                listings.size()
        );

        for (int i = start; i < end; i++) {
            AuctionListing listing = listings.get(i);

            int slot = i - start;

            ItemStack display = listing.getItem().clone();

            ItemMeta meta = display.getItemMeta();

            if (meta == null) {
                continue;
            }

            List<String> lore = meta.hasLore()
                    ? new ArrayList<>(meta.getLore())
                    : new ArrayList<>();

            lore.add("");
            lore.add("§8§m----------------------");
            lore.add("§7Seller: §f" + listing.getSellerName());
            lore.add("§7Price: §a$" + listing.getPrice());
            lore.add("");

            if (viewerUUID != null
                    && viewerUUID.equals(listing.getSellerUUID())) {

                lore.add("§a§lINI BARANG MILIKMU");
                lore.add("§eKlik untuk mengambil kembali barang ini.");

            } else {
                lore.add("§e§lKLIK UNTUK MEMBELI");
            }

            lore.add("§8§m----------------------");

            meta.setLore(lore);
            display.setItemMeta(meta);

            inventory.setItem(slot, display);
        }

        inventory.setItem(45, createButton(
                Material.ARROW,
                "§eHalaman Sebelumnya",
                "§7Klik untuk kembali."
        ));

        inventory.setItem(49, createButton(
                Material.BARRIER,
                "§cTutup",
                "§7Klik untuk menutup menu."
        ));

        inventory.setItem(53, createButton(
                Material.ARROW,
                "§eHalaman Berikutnya",
                "§7Klik untuk lanjut."
        ));

        inventory.setItem(48, createButton(
                Material.BOOK,
                "§bInformasi Auction House",
                "§7Total listing: §f" + listings.size(),
                "§7Halaman: §f" + (page + 1)
        ));
    }

    private void fillBackground() {
        ItemStack filler = createButton(
                Material.GRAY_STAINED_GLASS_PANE,
                "§7"
        );

        for (int i = 45; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack createButton(
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                meta.setLore(List.of(lore));
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}
