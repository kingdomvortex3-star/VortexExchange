
package id.vortexexchange.database;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class MailboxListener implements Listener {

    private final MailboxManager mailboxManager;
    private final JavaPlugin plugin;

    public MailboxListener(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
        this.plugin = JavaPlugin.getProvidingPlugin(
                MailboxListener.class
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getScheduler().runDelayed(
                plugin,
                task -> {
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
                            player.sendMessage(
                                    ChatColor.GREEN
                                            + "Kamu masih memiliki "
                                            + count
                                            + " barang di Mailbox!"
                            );

                            player.sendMessage(
                                    ChatColor.GRAY
                                            + "Kosongkan inventory untuk menerima semua barang."
                            );
                        }
                    } catch (SQLException exception) {
                        plugin.getLogger().severe(
                                "Gagal memeriksa mailbox pemain "
                                        + player.getUniqueId()
                                        + ": "
                                        + exception.getMessage()
                        );

                        player.sendMessage(
                                ChatColor.RED
                                        + "Mailbox gagal diperiksa. Hubungi admin."
                        );
                    }
                },
                null,
                40L
        );
    }
          }
