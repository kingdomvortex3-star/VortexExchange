
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

        inventory.setItem(17, createItem(
                Material.CYAN_STAINED_GLASS_PANE,
                "&b&lVORTEX"
        ));

        inventory.setItem(26, createItem(
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "&b&lEXCHANGE"
        ));

        inventory.setItem(35, createItem(
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "&b&lTRADE"
        ));
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
                "&a&lPENAWARAN UANG KAMU",
                "&7Jumlah: &f" + session.getMoney(viewer),
                "&7Status: " + readyStatus(viewer)
        ));

        inventory.setItem(49, createItem(
                Material.GOLD_INGOT,
                "&6&lPENAWARAN UANG LAWAN",
                "&7Jumlah: &f" + session.getMoney(opponent),
                "&7Status: " + readyStatus(opponent)
        ));

        inventory.setItem(8, createItem(
                Material.PLAYER_HEAD,
                "&b&lSTATUS TRADE",
                "&7Kamu: " + readyStatus(viewer),
                "&7Lawan: " + readyStatus(opponent)
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
                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slots[i], item);
        }
    }

    private void addControls() {
        inventory.setItem(45, createItem(
                Material.LIME_DYE,
                "&a&lREADY",
                "&7Klik untuk menyatakan siap.",
                "&7Status: " + readyStatus(viewer)
        ));

        inventory.setItem(53, createItem(
                Material.RED_DYE,
                "&c&lCANCEL",
                "&7Klik untuk membatalkan trade."
        ));

        inventory.setItem(46, createItem(
                Material.PAPER,
                "&e&lITEM KAMU",
                "&7Letakkan item dari inventory",
                "&7ke area penawaran di atas."
        ));

        inventory.setItem(52, createItem(
                Material.PAPER,
                "&b&lITEM LAWAN",
                "&7Item yang ditawarkan pemain lain."
        ));

        inventory.setItem(48, createItem(
                Material.LIME_STAINED_GLASS_PANE,
                "&a&lKONFIRMASI",
                "&7Kedua pemain harus READY."
        ));

        inventory.setItem(50, createItem(
                Material.RED_STAINED_GLASS_PANE,
                "&c&lBATAS TRADE",
                "&7Periksa item dan uang",
                "&7sebelum menyatakan READY."
        ));
    }

    private String readyStatus(UUID playerId) {
        return session.isReady(playerId)
                ? ChatColor.GREEN + "READY"
                : ChatColor.RED + "NOT READY";
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
