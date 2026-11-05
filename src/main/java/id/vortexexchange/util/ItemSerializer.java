
package id.vortexexchange.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public final class ItemSerializer {

    private ItemSerializer() {
    }

    public static String serialize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("Item tidak valid.");
        }

        byte[] data = item.serializeAsBytes();

        return Base64.getEncoder().encodeToString(data);
    }

    public static ItemStack deserialize(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException("Data item kosong.");
        }

        byte[] data = Base64.getDecoder().decode(encoded);

        return ItemStack.deserializeBytes(data);
    }
}
