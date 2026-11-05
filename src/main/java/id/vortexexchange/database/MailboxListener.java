
package id.vortexexchange.database;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class MailboxListener implements Listener {

    private final MailboxManager mailboxManager;

    public MailboxListener(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        player.getScheduler().runDelayed(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(
                        getClass()
                ),
                task -> {
                    mailboxManager.deliver(player);

                    try {
                        int count = mailboxManager.getItemCount(
                                player.getUniqueId()
                        );

                        if (count > 0) {
                            player.sendMessage(
                                    ChatColor.GREEN
                                            + "Kamu memiliki "
                                            + count
                                            + " barang di Mailbox!"
                            );
                        }
                    } catch (java.sql.SQLException exception) {
                        player.sendMessage(
                                ChatColor.RED
                                        + "Mailbox gagal diperiksa."
                        );
                    }
                },
                null,
                40L
        );
    }
}
