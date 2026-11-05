
package id.vortexexchange.trade;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.economy.EconomyManager;
import id.vortexexchange.util.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang().get("player.only"));
            return true;
        }

        if (!plugin.getConfig().getBoolean("trade.enabled", true)) {
            player.sendMessage(lang().get("trade.disabled"));
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
            player.sendMessage(lang().get("player.not-found"));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(lang().get("trade.self"));
            return;
        }

        if (tradeManager.isInTrade(player.getUniqueId())) {
            player.sendMessage(lang().get("trade.already-active"));
            return;
        }

        if (tradeManager.isInTrade(target.getUniqueId())) {
            player.sendMessage(lang().get("trade.target-active"));
            return;
        }

        if (plugin.getConfig().getBoolean("trade.require-same-world", true)
                && !player.getWorld().equals(target.getWorld())) {
            player.sendMessage(lang().get("trade.same-world"));
            return;
        }

        double maxDistance = plugin.getConfig().getDouble(
                "trade.max-distance", 15
        );

        if (player.getWorld().equals(target.getWorld())
                && player.getLocation().distance(target.getLocation()) > maxDistance) {
            player.sendMessage(lang().get("trade.too-far"));
            return;
        }

        if (tradeManager.areRequestsDisabled(target.getUniqueId())) {
            player.sendMessage(lang().get("trade.requests-disabled"));
            return;
        }

        if (!tradeManager.sendRequest(
                player.getUniqueId(),
                target.getUniqueId()
        )) {
            player.sendMessage(lang().get("trade.request-failed"));
            return;
        }

        player.sendMessage(lang().get(
                "trade.request-sent",
                "player", target.getName()
        ));

        target.sendMessage(lang().get(
                "trade.request-received",
                "player", player.getName()
        ));
        target.sendMessage(lang().get("trade.accept-hint"));
        target.sendMessage(lang().get("trade.deny-hint"));
    }

    private void accept(Player player) {
        UUID playerId = player.getUniqueId();

        if (tradeManager.isInTrade(playerId)) {
            player.sendMessage(lang().get("trade.already-active"));
            return;
        }

        int expireSeconds = Math.max(
                1,
                plugin.getConfig().getInt(
                        "trade.request-expire-seconds", 60
                )
        );

        TradeRequest request = tradeManager.getRequest(
                playerId,
                expireSeconds
        );

        if (request == null) {
            player.sendMessage(lang().get("trade.no-request"));
            return;
        }

        Player requester = Bukkit.getPlayer(request.getSender());

        if (requester == null || !requester.isOnline()) {
            tradeManager.denyRequest(playerId);
            player.sendMessage(lang().get("trade.requester-offline"));
            return;
        }

        TradeSession session = tradeManager.acceptRequest(
                playerId,
                expireSeconds
        );

        if (session == null) {
            player.sendMessage(lang().get("trade.accept-failed"));
            return;
        }

        player.sendMessage(lang().get(
                "trade.started",
                "player", requester.getName()
        ));

        requester.sendMessage(lang().get(
                "trade.accepted",
                "player", player.getName()
        ));

        openTrade(player, session);
        openTrade(requester, session);
    }

    private void deny(Player player) {
        int expireSeconds = Math.max(
                1,
                plugin.getConfig().getInt(
                        "trade.request-expire-seconds", 60
                )
        );

        TradeRequest request = tradeManager.getRequest(
                player.getUniqueId(),
                expireSeconds
        );

        if (request == null) {
            player.sendMessage(lang().get("trade.no-request"));
            return;
        }

        Player requester = Bukkit.getPlayer(request.getSender());

        tradeManager.denyRequest(player.getUniqueId());

        player.sendMessage(lang().get("trade.denied"));

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(lang().get(
                    "trade.denied-by",
                    "player", player.getName()
            ));
        }
    }

    private void toggle(Player player) {
        UUID uuid = player.getUniqueId();

        boolean disabled = !tradeManager.areRequestsDisabled(uuid);

        tradeManager.setRequestsDisabled(uuid, disabled);

        if (disabled) {
            player.sendMessage(lang().get("trade.toggle-off"));
        } else {
            player.sendMessage(lang().get("trade.toggle-on"));
        }
    }

    private void setMoney(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(lang().get("trade.help-money"));
            return;
        }

        if (!plugin.getConfig().getBoolean("trade.allow-money", true)) {
            player.sendMessage(lang().get("trade.money-disabled"));
            return;
        }

        if (!economyManager.isAvailable()) {
            player.sendMessage(lang().get("economy.missing"));
            return;
        }

        UUID uuid = player.getUniqueId();

        TradeSession session = tradeManager.getSession(uuid);

        if (session == null || !session.isActive()) {
            player.sendMessage(lang().get("trade.not-active"));
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(lang().get("trade.invalid-money"));
            return;
        }

        if (!Double.isFinite(amount) || amount < 0) {
            player.sendMessage(lang().get("trade.money-negative"));
            return;
        }

        if (!economyManager.has(player, amount)) {
            player.sendMessage(lang().get("trade.insufficient-money"));
            return;
        }

        double previousAmount = session.getMoney(uuid);

        session.setMoney(uuid, amount);

        if (!tradeManager.saveSession(session)) {
            session.setMoney(uuid, previousAmount);

            player.sendMessage(lang().get("trade.save-failed"));

            refreshTrade(session);
            return;
        }

        refreshTrade(session);

        player.sendMessage(lang().get(
                "trade.money-set",
                "amount", economyManager.format(amount)
        ));

        Player opponent = Bukkit.getPlayer(
                session.getOpponent(uuid)
        );

        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(lang().get(
                    "trade.money-updated",
                    "player", player.getName(),
                    "amount", economyManager.format(amount)
            ));
        }
    }

    private void openTrade(Player player, TradeSession session) {
        if (player == null || !player.isOnline()) {
            return;
        }

        TradeGUI gui = new TradeGUI(
                player.getUniqueId(),
                session
        );

        player.openInventory(gui.getInventory());
    }

    private void refreshTrade(TradeSession session) {
        refreshPlayer(session.getPlayerOne(), session);
        refreshPlayer(session.getPlayerTwo(), session);
    }

    private void refreshPlayer(
            UUID playerId,
            TradeSession session
    ) {
        Player player = Bukkit.getPlayer(playerId);

        if (player == null || !player.isOnline()) {
            return;
        }

        InventoryView view = player.getOpenInventory();
        Inventory top = view.getTopInventory();

        if (top.getHolder() instanceof TradeGUI) {
            TradeGUI updated = new TradeGUI(playerId, session);

            top.setContents(
                    updated.getInventory().getContents()
            );
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(lang().get("trade.help-title"));
        player.sendMessage(lang().get("trade.help-request"));
        player.sendMessage(lang().get("trade.help-accept"));
        player.sendMessage(lang().get("trade.help-deny"));
        player.sendMessage(lang().get("trade.help-toggle"));
        player.sendMessage(lang().get("trade.help-money"));
    }
}
