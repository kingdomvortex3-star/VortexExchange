
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.util.UUID;

public class TradeCommand implements CommandExecutor {

    private final VortexExchange plugin;
    private final TradeManager tradeManager;
    private final EconomyManager economyManager;

    public TradeCommand(
            VortexExchange plugin,
            TradeManager tradeManager,
            EconomyManager economyManager
    ) {
        this.plugin = plugin;
        this.tradeManager = tradeManager;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color("&cPerintah ini hanya untuk pemain."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("trade.enabled", true)) {
            player.sendMessage(message("trade-disabled"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "accept" -> accept(player);
            case "deny" -> deny(player);
            case "toggle" -> toggle(player);
            case "money" -> setMoney(player, args);
            default -> request(player, args);
        }

        return true;
    }

    private void request(Player player, String[] args) {
        if (args.length != 1) {
            sendHelp(player);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(message("player-not-found"));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(color("&cKamu tidak bisa trade dengan diri sendiri."));
            return;
        }

        if (tradeManager.isInTrade(player.getUniqueId())) {
            player.sendMessage(color("&cKamu sedang berada dalam trade."));
            return;
        }

        if (tradeManager.isInTrade(target.getUniqueId())) {
            player.sendMessage(color("&cPemain tersebut sedang melakukan trade."));
            return;
        }

        if (plugin.getConfig().getBoolean("trade.require-same-world", true)
                && !player.getWorld().equals(target.getWorld())) {
            player.sendMessage(color("&cPemain harus berada di world yang sama."));
            return;
        }

        double maxDistance = plugin.getConfig().getDouble("trade.max-distance", 15);

        if (player.getWorld().equals(target.getWorld())
                && player.getLocation().distance(target.getLocation()) > maxDistance) {
            player.sendMessage(color("&cPemain terlalu jauh."));
            return;
        }

        if (tradeManager.areRequestsDisabled(target.getUniqueId())) {
            player.sendMessage(color("&cPemain tersebut menonaktifkan permintaan trade."));
            return;
        }

        if (!tradeManager.sendRequest(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(color("&cPermintaan gagal dikirim. Coba lagi beberapa detik."));
            return;
        }

        player.sendMessage(color("&aPermintaan trade dikirim kepada &f"
                + target.getName() + "&a."));

        target.sendMessage(color("&b" + player.getName()
                + " &eingin melakukan trade denganmu."));
        target.sendMessage(color("&7Ketik &a/trade accept &7untuk menerima."));
        target.sendMessage(color("&7Ketik &c/trade deny &7untuk menolak."));
    }

    private void accept(Player player) {
        UUID playerId = player.getUniqueId();

        if (tradeManager.isInTrade(playerId)) {
            player.sendMessage(color("&cKamu sedang berada dalam trade."));
            return;
        }

        TradeRequest request = tradeManager.getRequest(playerId);

        if (request == null) {
            player.sendMessage(color("&cKamu tidak memiliki permintaan trade."));
            return;
        }

        Player requester = Bukkit.getPlayer(request.getSender());

        if (requester == null || !requester.isOnline()) {
            tradeManager.denyRequest(playerId);
            player.sendMessage(color("&cPemain yang mengirim permintaan sudah offline."));
            return;
        }

        TradeSession session = tradeManager.acceptRequest(playerId);

        if (session == null) {
            player.sendMessage(color("&cPermintaan trade gagal diterima."));
            return;
        }

        player.sendMessage(color("&aTrade dimulai dengan &f"
                + requester.getName() + "&a."));
        requester.sendMessage(color("&a" + player.getName()
                + " menerima trade kamu."));

        openTrade(player, session);
        openTrade(requester, session);
    }

    private void deny(Player player) {
        TradeRequest request = tradeManager.getRequest(player.getUniqueId());

        if (request == null) {
            player.sendMessage(color("&cKamu tidak memiliki permintaan trade."));
            return;
        }

        Player requester = Bukkit.getPlayer(request.getSender());

        tradeManager.denyRequest(player.getUniqueId());

        player.sendMessage(color("&cPermintaan trade ditolak."));

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(color("&c" + player.getName()
                    + " menolak permintaan trade."));
        }
    }

    private void toggle(Player player) {
        UUID uuid = player.getUniqueId();

        boolean disabled = !tradeManager.areRequestsDisabled(uuid);

        tradeManager.setRequestsDisabled(uuid, disabled);

        if (disabled) {
            player.sendMessage(color("&cPermintaan trade sekarang DINONAKTIFKAN."));
        } else {
            player.sendMessage(color("&aPermintaan trade sekarang DIAKTIFKAN."));
        }
    }

    private void setMoney(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(color("&cGunakan: /trade money <jumlah>"));
            return;
        }

        if (!plugin.getConfig().getBoolean("trade.allow-money", true)) {
            player.sendMessage(color("&cPenawaran uang sedang dinonaktifkan."));
            return;
        }

        if (!economyManager.isAvailable()) {
            player.sendMessage(message("economy-not-found"));
            return;
        }

        if (!tradeManager.isInTrade(player.getUniqueId())) {
            player.sendMessage(color("&cKamu tidak sedang melakukan trade."));
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(color("&cJumlah uang tidak valid."));
            return;
        }

        if (!Double.isFinite(amount) || amount < 0) {
            player.sendMessage(color("&cJumlah uang harus 0 atau lebih."));
            return;
        }

        if (!economyManager.has(player, amount)) {
            player.sendMessage(color("&cSaldo kamu tidak mencukupi."));
            return;
        }

        TradeSession session = tradeManager.getSession(player.getUniqueId());

        if (session == null || !session.isActive()) {
            player.sendMessage(color("&cSesi trade tidak ditemukan."));
            return;
        }

        session.setMoney(player.getUniqueId(), amount);
        session.resetReady();

        refreshTrade(session);

        player.sendMessage(color("&aPenawaran uang diatur menjadi &b"
                + economyManager.format(amount) + "&a."));

        UUID opponentId = session.getOpponent(player.getUniqueId());
        Player opponent = Bukkit.getPlayer(opponentId);

        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(color("&b" + player.getName()
                    + " &amengubah penawaran uang menjadi &b"
                    + economyManager.format(amount) + "&a."));
        }
    }

    private void openTrade(Player player, TradeSession session) {
        if (player == null || !player.isOnline()) {
            return;
        }

        TradeGUI gui = new TradeGUI(player.getUniqueId(), session);
        player.openInventory(gui.getInventory());
    }

    private void refreshTrade(TradeSession session) {
        refreshPlayer(session.getPlayerOne(), session);
        refreshPlayer(session.getOpponent(session.getPlayerOne()), session);
    }

    private void refreshPlayer(UUID playerId, TradeSession session) {
        Player player = Bukkit.getPlayer(playerId);

        if (player == null || !player.isOnline()) {
            return;
        }

        InventoryView view = player.getOpenInventory();
        Inventory top = view.getTopInventory();

        if (top.getHolder() instanceof TradeGUI) {
            TradeGUI updated = new TradeGUI(playerId, session);
            top.setContents(updated.getInventory().getContents());
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&b&lVortexExchange &8» &fPlayer Trade"));
        player.sendMessage(color("&7/trade <player> &8- &fKirim permintaan trade"));
        player.sendMessage(color("&7/trade accept &8- &fTerima permintaan"));
        player.sendMessage(color("&7/trade deny &8- &fTolak permintaan"));
        player.sendMessage(color("&7/trade toggle &8- &fAktif/nonaktif permintaan"));
        player.sendMessage(color("&7/trade money <jumlah> &8- &fTawarkan uang"));
    }

    private String message(String key) {
        return color(plugin.getConfig().getString(
                "messages." + key,
                "&cTerjadi kesalahan."
        ));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
