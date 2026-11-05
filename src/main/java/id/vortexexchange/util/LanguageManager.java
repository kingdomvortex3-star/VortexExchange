
package id.vortexexchange.util;

import id.vortexexchange.VortexExchange;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final VortexExchange plugin;
    private final Map<String, String> indonesia = new HashMap<>();
    private final Map<String, String> english = new HashMap<>();

    public LanguageManager(VortexExchange plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    private void loadLanguages() {

        indonesia.put("prefix", "&b&lVortexExchange &8»");
        indonesia.put("auction.success", "&aBarang berhasil dipasang di Auction House!");
        indonesia.put("auction.price", "&7Harga: &b{price}");
        indonesia.put("auction.new", "&eAUCTION BARU!");
        indonesia.put("auction.seller", "&f{player} &7sedang melelang barang!");
        indonesia.put("auction.item", "&7Barang: &b{amount}x {item}");
        indonesia.put("auction.view", "&eGunakan &b/ah &euntuk melihat dan membeli!");
        indonesia.put("auction.expired", "&cListing kamu telah kedaluwarsa!");
        indonesia.put("auction.returned", "&aBarang kamu telah dikembalikan ke Mailbox.");
        indonesia.put("auction.failed", "&cGagal memasang barang.");
        indonesia.put("auction.minimum", "&cHarga minimum: {price}");
        indonesia.put("auction.limit", "&cKamu sudah mencapai batas maksimal listing!");
        indonesia.put("permission.denied", "&cKamu tidak memiliki izin.");
        indonesia.put("player.only", "&cPerintah ini hanya untuk pemain.");
        indonesia.put("economy.missing", "&cVault atau plugin ekonomi tidak ditemukan.");
        indonesia.put("trade.success", "&aTransaksi berhasil!");
        indonesia.put("mailbox.empty", "&7Mailbox kamu kosong.");

        english.put("prefix", "&b&lVortexExchange &8»");
        english.put("auction.success", "&aYour item has been listed on the Auction House!");
        english.put("auction.price", "&7Price: &b{price}");
        english.put("auction.new", "&eNEW AUCTION!");
        english.put("auction.seller", "&f{player} &7is auctioning an item!");
        english.put("auction.item", "&7Item: &b{amount}x {item}");
        english.put("auction.view", "&eUse &b/ah &eto view and purchase!");
        english.put("auction.expired", "&cYour auction listing has expired!");
        english.put("auction.returned", "&aYour item has been returned to your Mailbox.");
        english.put("auction.failed", "&cFailed to create auction listing.");
        english.put("auction.minimum", "&cMinimum price: {price}");
        english.put("auction.limit", "&cYou have reached the maximum listing limit!");
        english.put("permission.denied", "&cYou do not have permission.");
        english.put("player.only", "&cThis command can only be used by players.");
        english.put("economy.missing", "&cVault or an economy plugin was not found.");
        english.put("trade.success", "&aTransaction successful!");
        english.put("mailbox.empty", "&7Your mailbox is empty.");
    }

    public String get(String key) {
        String language = plugin.getConfig().getString(
                "settings.language", "id"
        );

        Map<String, String> selected =
                language != null && language.equalsIgnoreCase("en")
                        ? english
                        : indonesia;

        String message = selected.get(key);

        if (message == null) {
            message = indonesia.getOrDefault(key, key);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String get(String key, String... replacements) {
        String message = get(key);

        if (replacements == null) {
            return message;
        }

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(
                    "{" + replacements[i] + "}",
                    replacements[i + 1]
            );
        }

        return message;
    }
}
