
package id.vortexexchange.database;

import id.vortexexchange.VortexExchange;
import id.vortexexchange.util.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

public class MailboxListener implements Listener {

    private final MailboxManager mailboxManager;
    private final VortexExchange plugin;

    public MailboxListener(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
        this.plugin = VortexExchange.getInstance();
    }

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getScheduler().runDelayed(
                plugin,
                task -> processMailbox(player),
                null,
                40L
        );
    }

    private void processMailbox(Player player) {
        if (!player.isOnline()) {
            return;
        }

        mailboxManager.deliver(player);

        if (!player.isOnline()) {
            return;
        }

        try {
            int count = mailboxManager.getItemCount(
                    player.getUniqueId()
            );

            if (count > 0) {
                player.sendMessage(lang().get(
                        "mailbox.remaining",
                        "count", String.valueOf(count)
                ));

                player.sendMessage(lang().get(
                        "mailbox.empty-inventory"
                ));
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Gagal memeriksa mailbox pemain "
                            + player.getUniqueId()
                            + ": "
                            + exception.getMessage()
            );

            player.sendMessage(lang().get("mailbox.error"));
        }
    }
}
