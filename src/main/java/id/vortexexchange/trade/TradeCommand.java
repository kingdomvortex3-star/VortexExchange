
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradeCommand implements CommandExecutor {

    private final VortexExchange plugin;
    private final TradeManager tradeManager;
    private final EconomyManager economyManager;

    private final Set<UUID> disabledRequests =
            ConcurrentHashMap.newKeySet();

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
            sender.sendMessage("Perintah ini hanya untuk pemain.");
            return true;
        }

        if (!player.hasPermission("vortexexchange.use")) {
            player.sendMessage(color("&cKamu tidak memiliki izin."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("trade.enabled", true)) {
            player.sendMessage(color("&cPlayer Trade sedang dinonaktifkan."));
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
            default -> request(player, args[0]);
        }

        return true;
    }

    private void request(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null || !target.isOnline()) {
            player.sendMessage(color("&cPemain tidak ditemukan atau offline."));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(color("&cKamu tidak bisa trade dengan diri sendiri."));
            return;
        }

        if (disabledRequests.contains(target.getUniqueId())) {
            player.sendMessage(color("&cPemain tersebut menonaktifkan permintaan trade."));
            return;
        }

        if (plugin.getConfig().getBoolean("trade.require-same-world", true)
                && !player.getWorld().equals(target.getWorld())) {
            player.sendMessage(color("&cKalian harus berada di dunia yang sama."));
            return;
        }

        double maxDistance = plugin.getConfig().getDouble(
                "trade.max-distance", 15
        );

        if (player.getWorld().equals(target.getWorld())
                && player.getLocation().distanceSquared(target.getLocation())
                > maxDistance * maxDistance) {
            player.sendMessage(color("&cPemain terlalu jauh."));
            return;
        }

        if (!tradeManager.sendRequest(
                player.getUniqueId(),
                target.getUniqueId()
        )) {
            player.sendMessage(color("&cKamu atau pemain tersebut sedang trade."));
            return;
        }

        player.sendMessage(color(
                "&aPermintaan trade dikirim ke &f" + target.getName()
        ));

        target.sendMessage(color(
                "&b" + player.getName()
                        + " &7ingin melakukan trade denganmu."
        ));

        target.sendMessage(color(
                "&aKetik /trade accept &7untuk menerima."
        ));

        target.sendMessage(color(
                "&cKetik /trade deny &7untuk menolak."
        ));
    }

    private void accept(Player player) {
        TradeSession session = tradeManager.acceptRequest(
                player.getUniqueId()
        );

        if (session == null) {
            player.sendMessage(color("&cTidak ada permintaan trade yang valid."));
            return;
        }

        Player sender = Bukkit.getPlayer(session.getPlayerOne());

        if (sender == null || !sender.isOnline()) {
            tradeManager.removeSession(session);
            player.sendMessage(color("&cPengirim sudah offline."));
            return;
        }

        player.sendMessage(color("&aTrade dimulai!"));
        sender.sendMessage(color("&aPermintaan trade diterima!"));

        openTrade(player, session);
        openTrade(sender, session);
    }

    private void deny(Player player) {
        TradeRequest request = tradeManager.getRequest(
                player.getUniqueId()
        );

        if (request == null || !tradeManager.denyRequest(
                player.getUniqueId()
        )) {
            player.sendMessage(color("&cTidak ada permintaan trade."));
            return;
        }

        Player sender = Bukkit.getPlayer(request.getSender());

        if (sender != null) {
            sender.sendMessage(color(
                    "&cPermintaan trade kamu ditolak."
            ));
        }

        player.sendMessage(color("&ePermintaan trade ditolak."));
    }

    private void toggle(Player player) {
        UUID uuid = player.getUniqueId();

        if (disabledRequests.remove(uuid)) {
            player.sendMessage(color("&aPermintaan trade sekarang aktif."));
        } else {
            disabledRequests.add(uuid);
            player.sendMessage(color("&cPermintaan trade sekarang nonaktif."));
        }
    }

    private void setMoney(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(color("&cGunakan: /trade money <jumlah>"));
            return;
        }

        if (!plugin.getConfig().getBoolean("trade.allow-money", true)) {
            player.sendMessage(color("&cPenawaran uang dinonaktifkan."));
            return;
        }

        if (!economyManager.isAvailable()) {
            player.sendMessage(color("&cVault atau ekonomi tidak tersedia."));
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
            player.sendMessage(color("&cJumlah uang tidak valid."));
            return;
        }

        TradeSession session = tradeManager.getSession(
                player.getUniqueId()
        );

        if (session == null || !session.isActive()) {
            player.sendMessage(color("&cKamu tidak sedang melakukan trade."));
            return;
        }

        if (amount > 0 && !economyManager.has(player, amount)) {
            player.sendMessage(color("&cSaldo kamu tidak cukup."));
            return;
        }

        session.setMoney(player.getUniqueId(), amount);

        player.sendMessage(color(
                "&aPenawaran uang kamu diatur menjadi &e"
                        + economyManager.format(amount)
        ));

        Player opponent = Bukkit.getPlayer(
                session.getOpponent(player.getUniqueId())
        );

        if (opponent != null) {
            opponent.sendMessage(color(
                    "&b" + player.getName()
                            + " &7mengubah penawaran uangnya."
            ));
        }
    }

    private void openTrade(Player player, TradeSession session) {
        TradeGUI gui = new TradeGUI(player.getUniqueId(), session);
        player.openInventory(gui.getInventory());
    }

    private void sendHelp(Player player) {
        player.sendMessage(color("&b&lVortexExchange &8- &fTrade"));
        player.sendMessage(color("&e/trade <player> &7- Kirim permintaan"));
        player.sendMessage(color("&e/trade accept &7- Terima permintaan"));
        player.sendMessage(color("&e/trade deny &7- Tolak permintaan"));
        player.sendMessage(color("&e/trade toggle &7- Atur permintaan"));
        player.sendMessage(color("&e/trade money <jumlah> &7- Atur uang"));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
