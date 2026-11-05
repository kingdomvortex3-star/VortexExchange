
package id.vortexexchange.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionListing {

    private final int id;
    private final UUID sellerUUID;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long createdAt;
    private final long expiresAt;

    public AuctionListing(
            int id,
            UUID sellerUUID,
            String sellerName,
            ItemStack item,
            double price,
            long createdAt,
            long expiresAt
    ) {
        this.id = id;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.clone();
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public int getId() {
        return id;
    }

    public UUID getSellerUUID() {
        return sellerUUID;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public double getPrice() {
        return price;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
