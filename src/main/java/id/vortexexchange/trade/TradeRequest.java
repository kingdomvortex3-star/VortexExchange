
package id.vortexexchange.trade;

import java.util.UUID;

public class TradeRequest {

    private final UUID sender;
    private final UUID target;
    private final long createdAt;

    public TradeRequest(UUID sender, UUID target) {
        if (sender == null || target == null) {
            throw new IllegalArgumentException("Sender dan target tidak boleh null.");
        }

        if (sender.equals(target)) {
            throw new IllegalArgumentException("Pemain tidak bisa mengirim trade kepada dirinya sendiri.");
        }

        this.sender = sender;
        this.target = target;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(int expireSeconds) {
        if (expireSeconds <= 0) {
            return true;
        }

        long expirationTime = expireSeconds * 1000L;
        return System.currentTimeMillis() - createdAt >= expirationTime;
    }
}
