
package id.vortexexchange.trade;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeSession {

    private final UUID playerOne;
    private final UUID playerTwo;

    private final Map<UUID, List<ItemStack>> items = new HashMap<>();
    private final Map<UUID, Double> money = new HashMap<>();
    private final Map<UUID, Boolean> ready = new HashMap<>();

    private boolean completed;
    private boolean cancelled;

    public TradeSession(UUID playerOne, UUID playerTwo) {
        if (playerOne == null || playerTwo == null
                || playerOne.equals(playerTwo)) {
            throw new IllegalArgumentException("Player trade tidak valid.");
        }

        this.playerOne = playerOne;
        this.playerTwo = playerTwo;

        items.put(playerOne, new ArrayList<>());
        items.put(playerTwo, new ArrayList<>());

        money.put(playerOne, 0.0);
        money.put(playerTwo, 0.0);

        ready.put(playerOne, false);
        ready.put(playerTwo, false);
    }

    public UUID getPlayerOne() {
        return playerOne;
    }

    public UUID getPlayerTwo() {
        return playerTwo;
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

    public synchronized List<ItemStack> getItems(UUID player) {
        List<ItemStack> playerItems = items.get(player);

        if (playerItems == null) {
            return new ArrayList<>();
        }

        List<ItemStack> result = new ArrayList<>();

        for (ItemStack item : playerItems) {
            result.add(item.clone());
        }

        return result;
    }

    public synchronized void setItems(UUID player, List<ItemStack> newItems) {
        if (!isParticipant(player) || !isActive()) {
            return;
        }

        List<ItemStack> copiedItems = new ArrayList<>();

        if (newItems != null) {
            for (ItemStack item : newItems) {
                if (item != null && !item.getType().isAir()) {
                    copiedItems.add(item.clone());
                }
            }
        }

        items.put(player, copiedItems);
        resetReady();
    }

    public synchronized boolean addItem(UUID player, ItemStack item) {
        if (!isParticipant(player) || !isActive()
                || item == null || item.getType().isAir()) {
            return false;
        }

        List<ItemStack> playerItems = items.get(player);

        if (playerItems.size() >= 14) {
            return false;
        }

        playerItems.add(item.clone());
        resetReady();

        return true;
    }

    public synchronized ItemStack removeItem(UUID player, int index) {
        if (!isParticipant(player) || !isActive()) {
            return null;
        }

        List<ItemStack> playerItems = items.get(player);

        if (index < 0 || index >= playerItems.size()) {
            return null;
        }

        ItemStack removed = playerItems.remove(index).clone();
        resetReady();

        return removed;
    }

    public synchronized double getMoney(UUID player) {
        return money.getOrDefault(player, 0.0);
    }

    public synchronized void setMoney(UUID player, double amount) {
        if (!isParticipant(player) || !isActive()
                || !Double.isFinite(amount) || amount < 0) {
            return;
        }

        money.put(player, amount);
        resetReady();
    }

    public synchronized boolean isReady(UUID player) {
        return Boolean.TRUE.equals(ready.get(player));
    }

    public synchronized void setReady(UUID player, boolean value) {
        if (!isParticipant(player) || !isActive()) {
            return;
        }

        ready.put(player, value);
    }

    public synchronized void toggleReady(UUID player) {
        if (!isParticipant(player) || !isActive()) {
            return;
        }

        ready.put(player, !isReady(player));
    }

    public synchronized void resetReady() {
        ready.put(playerOne, false);
        ready.put(playerTwo, false);
    }

    public synchronized boolean areBothReady() {
        return isReady(playerOne) && isReady(playerTwo);
    }

    public synchronized boolean isActive() {
        return !completed && !cancelled;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public synchronized void setCompleted(boolean completed) {
        this.completed = completed;

        if (completed) {
            this.cancelled = false;
        }
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    public synchronized void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;

        if (cancelled) {
            this.completed = false;
        }
    }

    private boolean isParticipant(UUID player) {
        return player != null
                && (playerOne.equals(player) || playerTwo.equals(player));
    }
}
