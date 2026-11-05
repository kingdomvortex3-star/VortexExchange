
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.database.MailboxManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class TradeListener implements Listener {

    private final VortexExchange plugin;
    private final TradeManager tradeManager;
    private final TradeTransactionManager transactionManager;
    private final MailboxManager mailboxManager;

    private static final int[] OWN_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    public TradeListener(
            VortexExchange plugin,
            TradeManager tradeManager,
            TradeTransactionManager transactionManager,
            MailboxManager mailboxManager
    ) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.transactionManager = transactionManager;
        this.mailboxManager = mailboxManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder()
                instanceof TradeGUI gui)) {
            return;
        }

        event.setCancelled(true);

        TradeSession session = gui.getSession();

        if (session == null
                || !session.isActive()
                || !gui.getViewer().equals(player.getUniqueId())) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0) {
            return;
        }

        int topSize = event.getView().getTopInventory().getSize();

        if (slot >= topSize) {
            if (event.getClickedInventory() == player.getInventory()) {
                addOffer(player, session, event);
            }
            return;
        }

        if (slot == 45) {
            toggleReady(player, session);
            return;
        }

        if (slot == 53) {
            cancelTrade(session, "&cTrade dibatalkan.");
            return;
        }

        if (gui.isOwnOfferSlot(slot)) {
            removeOffer(player, session, slot);
        }
    }

    private void addOffer(
            Player player,
            TradeSession session,
            InventoryClickEvent event
    ) {
        if (!plugin.getConfig().getBoolean(
                "trade.allow-items", true)) {
            player.sendMessage(color(
                    "&cPenawaran item sedang dinonaktifkan."
            ));
            return;
        }

        ClickType click = event.getClick();

        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        List<ItemStack> oldItems = session.getItems(uuid);

        if (oldItems.size() >= OWN_SLOTS.length) {
            player.sendMessage(color(
                    "&cSlot penawaran sudah penuh."
            ));
            return;
        }

        if (!session.addItem(uuid, clicked.clone())) {
            player.sendMessage(color(
                    "&cItem gagal ditambahkan ke penawaran."
            ));
            return;
        }

        if (!tradeManager.saveSession(session)) {
            session.setItems(uuid, oldItems);

            player.sendMessage(color(
                    "&cGagal menyimpan penawaran ke database. "
                            + "Item tidak jadi ditambahkan."
            ));

            return;
        }

        event.setCurrentItem(null);

        refresh(session);

        player.sendMessage(color(
                "&aItem berhasil ditambahkan ke penawaran."
        ));
    }

    private void removeOffer(
            Player player,
            TradeSession session,
            int slot
    ) {
        int index = -1;

        for (int i = 0; i < OWN_SLOTS.length; i++) {
            if (OWN_SLOTS[i] == slot) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            return;
        }

        UUID uuid = player.getUniqueId();

        List<ItemStack> oldItems = session.getItems(uuid);

        ItemStack item = session.removeItem(uuid, index);

        if (item == null) {
            return;
        }

        if (!tradeManager.saveSession(session)) {
            session.setItems(uuid, oldItems);

            player.sendMessage(color(
                    "&cGagal menyimpan perubahan ke database."
            ));

            refresh(session);
            return;
        }

        var leftovers = player.getInventory().addItem(item.clone());

        leftovers.values().forEach(leftover ->
                player.getWorld().dropItemNaturally(
                        player.getLocation(),
                        leftover
                )
        );

        refresh(session);
    }

    private void toggleReady(
            Player player,
            TradeSession session
    ) {
        if (!session.isActive()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        session.toggleReady(uuid);

        if (session.areBothReady()) {
            notifyPlayers(
                    session,
                    "&aKedua pemain READY! Memproses transaksi..."
            );

            transactionManager.complete(session);
        } else {
            notifyPlayers(
                    session,
                    "&e" + player.getName()
                            + (session.isReady(uuid)
                            ? " &asudah READY."
                            : " &cmembatalkan READY.")
            );
        }

        if (session.isActive()) {
            refresh(session);
        }
    }

    private void cancelTrade(
            TradeSession session,
            String message
    ) {
        if (session == null || !session.isActive()) {
            return;
        }

        session.setCancelled(true);
        tradeManager.removeSession(session);

        returnItems(
                session.getPlayerOne(),
                session.getItems(session.getPlayerOne())
        );

        returnItems(
                session.getPlayerTwo(),
                session.getItems(session.getPlayerTwo())
        );

        if (!tradeManager.deleteStoredSession(session)) {
            plugin.getLogger().severe(
                    "GAGAL MENGHAPUS DATA TRADE DARI DATABASE: "
                            + session.getPlayerOne()
                            + " <-> "
                            + session.getPlayerTwo()
            );
        }

        notifyPlayers(session, message);

        closePlayer(session.getPlayerOne());
        closePlayer(session.getPlayerTwo());
    }

    private void returnItems(
            UUID uuid,
            List<ItemStack> items
    ) {
        Player player = Bukkit.getPlayer(uuid);

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            if (player == null || !player.isOnline()) {
                if (!mailboxManager.store(uuid, item.clone())) {
                    plugin.getLogger().severe(
                            "GAGAL MENYIMPAN ITEM TRADE KE MAILBOX: "
                                    + uuid
                    );
                }
                continue;
            }

            var leftovers = player.getInventory().addItem(item.clone());

            leftovers.values().forEach(leftover ->
                    player.getWorld().dropItemNaturally(
                            player.getLocation(),
                            leftover
                    )
            );
        }
    }

    private void refresh(TradeSession session) {
        refreshPlayer(session.getPlayerOne(), session);
        refreshPlayer(session.getPlayerTwo(), session);
    }

    private void refreshPlayer(
            UUID uuid,
            TradeSession session
    ) {
        Player player = Bukkit.getPlayer(uuid);

        if (player == null || !player.isOnline()) {
            return;
        }

        Inventory current =
                player.getOpenInventory().getTopInventory();

        if (!(current.getHolder() instanceof TradeGUI)) {
            return;
        }

        TradeGUI updated = new TradeGUI(uuid, session);

        current.setContents(
                updated.getInventory().getContents()
        );
    }

    private void notifyPlayers(
            TradeSession session,
            String message
    ) {
        for (UUID uuid : List.of(
                session.getPlayerOne(),
                session.getPlayerTwo()
        )) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null && player.isOnline()) {
                player.sendMessage(color(message));
            }
        }
    }

    private void closePlayer(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null
                && player.isOnline()
                && player.getOpenInventory()
                .getTopInventory().getHolder() instanceof TradeGUI) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder()
                instanceof TradeGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder()
                instanceof TradeGUI gui)) {
            return;
        }

        TradeSession session = gui.getSession();

        if (session != null && session.isActive()) {
            cancelTrade(
                    session,
                    "&cTrade dibatalkan karena GUI ditutup."
            );
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        TradeSession session = tradeManager.getSession(uuid);

        if (session != null && session.isActive()) {
            cancelTrade(
                    session,
                    "&cTrade dibatalkan karena pemain disconnect."
            );
        }

        tradeManager.removeRequests(uuid);
        tradeManager.clearPlayer(uuid);
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
