
package id.vortexexchange.util;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

public final class ItemSerializer {

    private static final int MAX_DATA_SIZE = 1_048_576;

    private ItemSerializer() {
    }

    public static String serialize(ItemStack item) {
        if (item == null
                || item.getType().isAir()
                || item.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Item tidak valid."
            );
        }

        try {
            byte[] data = item.serializeAsBytes();

            if (data.length == 0 || data.length > MAX_DATA_SIZE) {
                throw new IllegalArgumentException(
                        "Ukuran data item tidak valid."
                );
            }

            return Base64.getEncoder().encodeToString(data);

        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Gagal menyimpan data item.",
                    exception
            );
        }
    }

    public static ItemStack deserialize(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException(
                    "Data item kosong."
            );
        }

        try {
            byte[] data = Base64.getDecoder().decode(encoded);

            if (data.length == 0 || data.length > MAX_DATA_SIZE) {
                throw new IllegalArgumentException(
                        "Ukuran data item tidak valid."
                );
            }

            ItemStack item = ItemStack.deserializeBytes(data);

            if (item == null
                    || item.getType().isAir()
                    || item.getAmount() <= 0) {
                throw new IllegalArgumentException(
                        "Hasil pemulihan item tidak valid."
                );
            }

            return item;

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Data item rusak atau tidak valid.",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Gagal memulihkan data item.",
                    exception
            );
        }
    }
}
