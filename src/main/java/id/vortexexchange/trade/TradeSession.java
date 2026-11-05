
package id.vortexexchange.trade;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeSession {

    private final UUID playerOne;
    private final UUID playerTwo;

    private final List<ItemStack> itemsOne = new ArrayList<>();
    private final List<ItemStack> itemsTwo = new ArrayList<>();

    private double moneyOne;
    private double moneyTwo;

    private boolean readyOne;
    private boolean readyTwo;

    private boolean completed;
    private boolean cancelled;

    public TradeSession(UUID playerOne, UUID playerTwo) {
        if (playerOne == null || playerTwo == null
                || playerOne.equals(playerTwo)) {
            throw new IllegalArgumentException("Pemain trade tidak valid.");
        }

        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
    }

    public UUID getPlayerOne() {
        return playerOne;
    }

    public UUID getPlayerTwo() {
        return playerTwo;
    }

    public boolean contains(UUID player) {
        return playerOne.equals(player) || playerTwo.equals(player);
    }

    public UUID getOpponent(UUID player) {
        if (playerOne.equals(player)) {
            return playerTwo;
        }

        if (playerTwo.equals(player)) {
            return playerOne;
        }

        return null;
    }

    public List<ItemStack> getItems(UUID player) {
        List<ItemStack> source = getSource(player);
        List<ItemStack> copy = new ArrayList<>();

        for (ItemStack item : source) {
            copy.add(item.clone());
        }

        return copy;
    }

    public void setItems(UUID player, List<ItemStack> items) {
        List<ItemStack> target = getSource(player);

        target.clear();

        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                target.add(item.clone());
            }
        }

        resetReady();
    }

    public double getMoney(UUID player) {
        if (playerOne.equals(player)) {
            return moneyOne;
        }

        if (playerTwo.equals(player)) {
            return moneyTwo;
        }

        return 0;
    }

    public void setMoney(UUID player, double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Jumlah uang tidak valid.");
        }

        if (playerOne.equals(player)) {
            moneyOne = amount;
        } else if (playerTwo.equals(player)) {
            moneyTwo = amount;
        } else {
            throw new IllegalArgumentException("Pemain bukan anggota trade.");
        }

        resetReady();
    }

    public boolean isReady(UUID player) {
        if (playerOne.equals(player)) {
            return readyOne;
        }

        if (playerTwo.equals(player)) {
            return readyTwo;
        }

        return false;
    }

    public void setReady(UUID player, boolean ready) {
        if (playerOne.equals(player)) {
            readyOne = ready;
        } else if (playerTwo.equals(player)) {
            readyTwo = ready;
        }
    }

    public boolean bothReady() {
        return readyOne && readyTwo;
    }

    public void resetReady() {
        readyOne = false;
        readyTwo = false;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isActive() {
        return !completed && !cancelled;
    }

    private List<ItemStack> getSource(UUID player) {
        if (playerOne.equals(player)) {
            return itemsOne;
        }

        if (playerTwo.equals(player)) {
            return itemsTwo;
        }

        throw new IllegalArgumentException("Pemain bukan anggota trade.");
    }
}
