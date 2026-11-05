
package id.vortexexchange.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeGUI implements InventoryHolder {

    private final Inventory inventory;
    private final UUID viewer;
    private final TradeSession session;

    private static final int[] OWN_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private static final int[] OTHER_SLOTS = {
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public TradeGUI(UUID viewer, TradeSession session) {
        this.viewer = viewer;
        this.session = session;

        inventory = Bukkit.createInventory(
                this,
                54,
                ChatColor.DARK_GRAY + "Trade - "
                        + ChatColor.AQUA + "VortexExchange"
        );

        fillBackground();
        displayOffers();
        addControls();
    }

    private void fillBackground() {
        ItemStack glass = createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                " "
        );

        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45
                    || slot % 9 == 0 || slot % 9 == 8) {
                inventory.setItem(slot, glass);
            }
        }
    }

    private void displayOffers() {
        UUID opponent = session.getOpponent(viewer);

        if (opponent == null) {
            return;
        }

        displayItems(session.getItems(viewer), OWN_SLOTS);
        displayItems(session.getItems(opponent), OTHER_SLOTS);

        inventory.setItem(4, createItem(
                Material.EMERALD,
                "&aUang kamu: &f" + session.getMoney(viewer)
        ));

        inventory.setItem(49, createItem(
                Material.GOLD_INGOT,
                "&6Uang lawan: &f" + session.getMoney(opponent)
        ));
    }

    private void displayItems(List<ItemStack> items, int[] slots) {
        for (int i = 0; i < items.size() && i < slots.length; i++) {
            ItemStack item = items.get(i).clone();
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore()
                        ? new ArrayList<>(meta.getLore())
                        : new ArrayList<>();

                lore.add(ChatColor.GRAY + "Penawaran Trade");
                item.setItemMeta(meta);
            }

            inventory.setItem(slots[i], item);
        }
    }

    private void addControls() {
        inventory.setItem(45, createItem(
                Material.LIME_DYE,
                "&a&lREADY",
                "&7Klik untuk menyatakan siap."
        ));

        inventory.setItem(53, createItem(
                Material.RED_DYE,
                "&c&lCANCEL",
                "&7Klik untuk membatalkan trade."
        ));

        inventory.setItem(46, createItem(
                Material.PAPER,
                "&ePenawaran Kamu"
        ));

        inventory.setItem(52, createItem(
                Material.PAPER,
                "&bPenawaran Lawan"
        ));
    }

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(
                    ChatColor.translateAlternateColorCodes('&', name)
            );

            if (lore.length > 0) {
                List<String> lines = new ArrayList<>();

                for (String line : lore) {
                    lines.add(
                            ChatColor.translateAlternateColorCodes('&', line)
                    );
                }

                meta.setLore(lines);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public UUID getViewer() {
        return viewer;
    }

    public TradeSession getSession() {
        return session;
    }

    public boolean isOwnOfferSlot(int slot) {
        for (int ownSlot : OWN_SLOTS) {
            if (ownSlot == slot) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
