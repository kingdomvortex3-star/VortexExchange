
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

        // INDONESIAN

        indonesia.put("permission.denied", "&cKamu tidak memiliki izin.");
        indonesia.put("player.only", "&cPerintah ini hanya untuk pemain.");
        indonesia.put("player.not-found", "&cPemain tidak ditemukan.");
        indonesia.put("economy.missing", "&cVault atau plugin ekonomi tidak ditemukan.");

        // AUCTION HOUSE

        indonesia.put("auction.disabled", "&cAuction House sedang dinonaktifkan.");
        indonesia.put("auction.success", "&aBarang berhasil dipasang di Auction House!");
        indonesia.put("auction.price", "&7Harga: &b{price}");
        indonesia.put("auction.new", "&eAUCTION BARU!");
        indonesia.put("auction.seller", "&f{player} &7sedang melelang barang!");
        indonesia.put("auction.item", "&7Barang: &b{amount}x {item}");
        indonesia.put("auction.view", "&eGunakan &b/ah &euntuk melihat dan membeli!");
        indonesia.put("auction.expired", "&cListing kamu telah kedaluwarsa!");
        indonesia.put("auction.returned", "&aBarang kamu dikembalikan ke Mailbox.");
        indonesia.put("auction.failed", "&cGagal memasang barang.");
        indonesia.put("auction.minimum", "&cHarga minimum: {price}");
        indonesia.put("auction.limit", "&cKamu sudah mencapai batas maksimal listing!");
        indonesia.put("auction.processing", "&cListing sedang diproses.");
        indonesia.put("auction.unavailable", "&cListing sudah tidak tersedia.");
        indonesia.put("auction.own-item", "&cKamu tidak bisa membeli barang sendiri.");
        indonesia.put("auction.invalid-seller", "&cData penjual tidak valid.");
        indonesia.put("auction.invalid-item", "&cData barang tidak valid. Hubungi admin.");
        indonesia.put("auction.invalid-price", "&cHarga listing tidak valid.");
        indonesia.put("auction.no-balance", "&cSaldo kamu tidak cukup.");
        indonesia.put("auction.payment-failed", "&cPembayaran gagal.");
        indonesia.put("auction.purchase-success", "&aBerhasil membeli barang seharga &e{price}");
        indonesia.put("auction.sold", "&aBarang kamu dibeli oleh &f{player}");
        indonesia.put("auction.received-money", "&aKamu menerima &e{price}");
        indonesia.put("auction.transaction-error", "&cTransaksi bermasalah. Hubungi admin.");
        indonesia.put("auction.delivery-error", "&cPembayaran berhasil, tetapi pengiriman barang bermasalah. Hubungi admin.");
        indonesia.put("auction.database-error", "&cTerjadi kesalahan database.");
        indonesia.put("auction.general-error", "&cTerjadi kesalahan saat membeli barang. Hubungi admin.");
        indonesia.put("auction.usage", "&cGunakan: /ah atau /ah sell <harga>");
        indonesia.put("auction.hold-item", "&cPegang barang yang ingin dijual.");
        indonesia.put("auction.duration-invalid", "&cDurasi listing tidak valid.");
        indonesia.put("auction.listing-count", "&7Listing aktif: &b{current}&7/&b{maximum}");

        // PLAYER TRADE

        indonesia.put("trade.disabled", "&cPlayer Trade sedang dinonaktifkan.");
        indonesia.put("trade.usage", "&cGunakan perintah trade dengan benar.");
        indonesia.put("trade.self", "&cKamu tidak bisa trade dengan diri sendiri.");
        indonesia.put("trade.already-active", "&cKamu sedang berada dalam trade.");
        indonesia.put("trade.target-active", "&cPemain tersebut sedang melakukan trade.");
        indonesia.put("trade.same-world", "&cPemain harus berada di world yang sama.");
        indonesia.put("trade.too-far", "&cPemain terlalu jauh.");
        indonesia.put("trade.requests-disabled", "&cPemain tersebut menonaktifkan permintaan trade.");
        indonesia.put("trade.request-failed", "&cPermintaan gagal dikirim. Coba lagi beberapa detik.");
        indonesia.put("trade.request-sent", "&aPermintaan trade dikirim kepada &f{player}&a.");
        indonesia.put("trade.request-received", "&b{player} &eingin melakukan trade denganmu.");
        indonesia.put("trade.accept-hint", "&7Ketik &a/trade accept &7untuk menerima.");
        indonesia.put("trade.deny-hint", "&7Ketik &c/trade deny &7untuk menolak.");
        indonesia.put("trade.no-request", "&cPermintaan trade tidak ada atau sudah kedaluwarsa.");
        indonesia.put("trade.requester-offline", "&cPemain yang mengirim permintaan sudah offline.");
        indonesia.put("trade.accept-failed", "&cPermintaan trade gagal diterima. Database mungkin bermasalah.");
        indonesia.put("trade.started", "&aTrade dimulai dengan &f{player}&a.");
        indonesia.put("trade.accepted", "&a{player} menerima trade kamu.");
        indonesia.put("trade.denied", "&cPermintaan trade ditolak.");
        indonesia.put("trade.denied-by", "&c{player} menolak permintaan trade.");
        indonesia.put("trade.toggle-on", "&aPermintaan trade sekarang DIAKTIFKAN.");
        indonesia.put("trade.toggle-off", "&cPermintaan trade sekarang DINONAKTIFKAN.");
        indonesia.put("trade.not-active", "&cKamu tidak sedang melakukan trade.");
        indonesia.put("trade.money-disabled", "&cPenawaran uang sedang dinonaktifkan.");
        indonesia.put("trade.invalid-money", "&cJumlah uang tidak valid.");
        indonesia.put("trade.money-negative", "&cJumlah uang harus 0 atau lebih.");
        indonesia.put("trade.insufficient-money", "&cSaldo kamu tidak mencukupi.");
        indonesia.put("trade.save-failed", "&cGagal menyimpan penawaran uang ke database. Perubahan dibatalkan.");
        indonesia.put("trade.money-set", "&aPenawaran uang diatur menjadi &b{amount}&a.");
        indonesia.put("trade.money-updated", "&b{player} &amengubah penawaran uang menjadi &b{amount}&a.");
        indonesia.put("trade.help-title", "&b&lVortexExchange &8» &fPlayer Trade");
        indonesia.put("trade.help-request", "&7/trade <player> &8- &fKirim permintaan trade");
        indonesia.put("trade.help-accept", "&7/trade accept &8- &fTerima permintaan");
        indonesia.put("trade.help-deny", "&7/trade deny &8- &fTolak permintaan");
        indonesia.put("trade.help-toggle", "&7/trade toggle &8- &fAktif/nonaktif permintaan");
        indonesia.put("trade.help-money", "&7/trade money <jumlah> &8- &fTawarkan uang");

        indonesia.put("trade.cancelled", "&cTrade dibatalkan.");
        indonesia.put("trade.items-disabled", "&cPenawaran barang sedang dinonaktifkan.");
        indonesia.put("trade.slots-full", "&cSlot penawaran barang sudah penuh.");
        indonesia.put("trade.item-add-failed", "&cGagal menambahkan barang ke penawaran.");
        indonesia.put("trade.item-save-failed", "&cGagal menyimpan penawaran barang. Perubahan dibatalkan.");
        indonesia.put("trade.item-added", "&aBarang berhasil ditambahkan ke penawaran.");
        indonesia.put("trade.both-ready", "&aKedua pemain siap. Memproses trade...");
        indonesia.put("trade.player-ready", "&a{player} sudah siap.");
        indonesia.put("trade.player-unready", "&e{player} membatalkan status siap.");
        indonesia.put("trade.gui-closed", "&cTrade dibatalkan karena GUI ditutup.");
        indonesia.put("trade.disconnected", "&cTrade dibatalkan karena pemain terputus.");
        indonesia.put("trade.player-offline", "&cSalah satu pemain sedang offline.");
        indonesia.put("trade.invalid-items", "&cData barang trade tidak valid.");
        indonesia.put("trade.payment-failed", "&cPembayaran trade gagal.");
        indonesia.put("trade.rollback-failed", "&cPengembalian uang gagal. Hubungi admin.");
        indonesia.put("trade.delivery-error", "&cPengiriman barang trade bermasalah. Hubungi admin.");
        indonesia.put("trade.completed", "&aTrade berhasil diselesaikan!");
        indonesia.put("trade.completed-with", "&aTrade dengan &f{player} &aberhasil diselesaikan!");

        // MAILBOX

        indonesia.put("mailbox.remaining", "&eKamu masih memiliki &b{count} &ebarang di Mailbox!");
        indonesia.put("mailbox.empty-inventory", "&7Kosongkan inventory untuk menerima semua barang.");
        indonesia.put("mailbox.error", "&cMailbox gagal diperiksa. Hubungi admin.");
        indonesia.put("mailbox.delivered", "&aMailbox: &f{count} &aitem berhasil diterima.");

        // ENGLISH

        english.put("permission.denied", "&cYou do not have permission.");
        english.put("player.only", "&cThis command can only be used by players.");
        english.put("player.not-found", "&cPlayer not found.");
        english.put("economy.missing", "&cVault or an economy plugin was not found.");

        // AUCTION HOUSE

        english.put("auction.disabled", "&cAuction House is disabled.");
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
        english.put("auction.processing", "&cThis listing is currently being processed.");
        english.put("auction.unavailable", "&cThis listing is no longer available.");
        english.put("auction.own-item", "&cYou cannot buy your own item.");
        english.put("auction.invalid-seller", "&cInvalid seller data.");
        english.put("auction.invalid-item", "&cInvalid item data. Contact an administrator.");
        english.put("auction.invalid-price", "&cInvalid listing price.");
        english.put("auction.no-balance", "&cYou do not have enough money.");
        english.put("auction.payment-failed", "&cPayment failed.");
        english.put("auction.purchase-success", "&aSuccessfully purchased the item for &e{price}");
        english.put("auction.sold", "&aYour item was purchased by &f{player}");
        english.put("auction.received-money", "&aYou received &e{price}");
        english.put("auction.transaction-error", "&cTransaction problem. Contact an administrator.");
        english.put("auction.delivery-error", "&cPayment succeeded, but item delivery failed. Contact an administrator.");
        english.put("auction.database-error", "&cA database error occurred.");
        english.put("auction.general-error", "&cAn error occurred while purchasing. Contact an administrator.");
        english.put("auction.usage", "&cUsage: /ah or /ah sell <price>");
        english.put("auction.hold-item", "&cHold the item you want to sell.");
        english.put("auction.duration-invalid", "&cInvalid listing duration.");
        english.put("auction.listing-count", "&7Active listings: &b{current}&7/&b{maximum}");

        // PLAYER TRADE

        english.put("trade.disabled", "&cPlayer Trading is disabled.");
        english.put("trade.usage", "&cPlease use the trade command correctly.");
        english.put("trade.self", "&cYou cannot trade with yourself.");
        english.put("trade.already-active", "&cYou are already in a trade.");
        english.put("trade.target-active", "&cThat player is already trading.");
        english.put("trade.same-world", "&cThe player must be in the same world.");
        english.put("trade.too-far", "&cThat player is too far away.");
        english.put("trade.requests-disabled", "&cThat player has disabled trade requests.");
        english.put("trade.request-failed", "&cFailed to send trade request. Try again in a few seconds.");
        english.put("trade.request-sent", "&aTrade request sent to &f{player}&a.");
        english.put("trade.request-received", "&b{player} &ewants to trade with you.");
        english.put("trade.accept-hint", "&7Type &a/trade accept &7to accept.");
        english.put("trade.deny-hint", "&7Type &c/trade deny &7to decline.");
        english.put("trade.no-request", "&cNo trade request found or it has expired.");
        english.put("trade.requester-offline", "&cThe player who sent the request is now offline.");
        english.put("trade.accept-failed", "&cFailed to accept trade. The database may be unavailable.");
        english.put("trade.started", "&aTrade started with &f{player}&a.");
        english.put("trade.accepted", "&a{player} accepted your trade.");
        english.put("trade.denied", "&cTrade request denied.");
        english.put("trade.denied-by", "&c{player} denied your trade request.");
        english.put("trade.toggle-on", "&aTrade requests are now ENABLED.");
        english.put("trade.toggle-off", "&cTrade requests are now DISABLED.");
        english.put("trade.not-active", "&cYou are not currently trading.");
        english.put("trade.money-disabled", "&cMoney offers are disabled.");
        english.put("trade.invalid-money", "&cInvalid money amount.");
        english.put("trade.money-negative", "&cMoney amount must be 0 or greater.");
        english.put("trade.insufficient-money", "&cYou do not have enough money.");
        english.put("trade.save-failed", "&cFailed to save money offer. Changes reverted.");
        english.put("trade.money-set", "&aYour money offer is now &b{amount}&a.");
        english.put("trade.money-updated", "&b{player} &aupdated their money offer to &b{amount}&a.");
        english.put("trade.help-title", "&b&lVortexExchange &8» &fPlayer Trade");
        english.put("trade.help-request", "&7/trade <player> &8- &fSend a trade request");
        english.put("trade.help-accept", "&7/trade accept &8- &fAccept a request");
        english.put("trade.help-deny", "&7/trade deny &8- &fDecline a request");
        english.put("trade.help-toggle", "&7/trade toggle &8- &fEnable/disable requests");
        english.put("trade.help-money", "&7/trade money <amount> &8- &fOffer money");

        english.put("trade.cancelled", "&cTrade cancelled.");
        english.put("trade.items-disabled", "&cItem offers are disabled.");
        english.put("trade.slots-full", "&cItem offer slots are full.");
        english.put("trade.item-add-failed", "&cFailed to add item to the offer.");
        english.put("trade.item-save-failed", "&cFailed to save item offer. Changes reverted.");
        english.put("trade.item-added", "&aItem added to the offer.");
        english.put("trade.both-ready", "&aBoth players are ready. Processing trade...");
        english.put("trade.player-ready", "&a{player} is ready.");
        english.put("trade.player-unready", "&e{player} is no longer ready.");
        english.put("trade.gui-closed", "&cTrade cancelled because the GUI was closed.");
        english.put("trade.disconnected", "&cTrade cancelled because a player disconnected.");
        english.put("trade.player-offline", "&cOne of the players is offline.");
        english.put("trade.invalid-items", "&cTrade item data is invalid.");
        english.put("trade.payment-failed", "&cTrade payment failed.");
        english.put("trade.rollback-failed", "&cMoney rollback failed. Contact an administrator.");
        english.put("trade.delivery-error", "&cTrade item delivery failed. Contact an administrator.");
        english.put("trade.completed", "&aTrade completed successfully!");
        english.put("trade.completed-with", "&aTrade with &f{player} &acompleted successfully!");

        // MAILBOX

        english.put("mailbox.remaining", "&eYou still have &b{count} &eitems in your Mailbox!");
        english.put("mailbox.empty-inventory", "&7Clear your inventory to receive all items.");
        english.put("mailbox.error", "&cMailbox could not be checked. Contact an administrator.");
        english.put("mailbox.delivered", "&aMailbox: &f{count} &aitems delivered successfully.");
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
