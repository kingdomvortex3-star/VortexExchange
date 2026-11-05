
package id.vortexexchange.trade;

import java.util.UUID;

public class TradeRequest {

    private final UUID sender;
    private final UUID target;
    private final long createdAt;

    public TradeRequest(UUID sender, UUID target) {
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

    public boolean isExpired(long expireSeconds) {
        return System.currentTimeMillis() - createdAt
                >= expireSeconds * 1000L;
    }
}
