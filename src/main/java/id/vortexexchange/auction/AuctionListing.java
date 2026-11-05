
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
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "ID listing harus lebih dari 0."
            );
        }

        if (sellerUUID == null) {
            throw new IllegalArgumentException(
                    "UUID penjual tidak boleh kosong."
            );
        }

        if (sellerName == null || sellerName.isBlank()) {
            throw new IllegalArgumentException(
                    "Nama penjual tidak valid."
            );
        }

        if (item == null || item.getType().isAir()
                || item.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Item listing tidak valid."
            );
        }

        if (!Double.isFinite(price) || price <= 0) {
            throw new IllegalArgumentException(
                    "Harga listing tidak valid."
            );
        }

        if (createdAt <= 0 || expiresAt <= createdAt) {
            throw new IllegalArgumentException(
                    "Waktu listing tidak valid."
            );
        }

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

    public long getRemainingTime() {
        return Math.max(
                0L,
                expiresAt - System.currentTimeMillis()
        );
    }
}
