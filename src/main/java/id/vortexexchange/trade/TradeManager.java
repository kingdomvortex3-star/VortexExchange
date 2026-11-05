
package id.vortexexchange.trade;

import id.vortexexchange.database.TradeStorage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final Map<UUID, TradeRequest> requests =
            new ConcurrentHashMap<>();

    private final Map<UUID, TradeSession> sessions =
            new ConcurrentHashMap<>();

    private final Map<UUID, Long> requestCooldowns =
            new ConcurrentHashMap<>();

    private final Map<UUID, Boolean> disabledRequests =
            new ConcurrentHashMap<>();

    private volatile TradeStorage storage;

    public TradeManager() {
    }

    public TradeManager(TradeStorage storage) {
        this.storage = storage;
    }

    public void setStorage(TradeStorage storage) {
        this.storage = storage;
    }

    public boolean saveSession(TradeSession session) {
        if (session == null || !session.isActive()) {
            return false;
        }

        TradeStorage currentStorage = storage;

        if (currentStorage == null) {
            return false;
        }

        try {
            currentStorage.save(session);
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    public boolean deleteStoredSession(TradeSession session) {
        if (session == null) {
            return false;
        }

        TradeStorage currentStorage = storage;

        if (currentStorage == null) {
            return false;
        }

        try {
            return currentStorage.delete(session);
        } catch (SQLException exception) {
            return false;
        }
    }

    public synchronized boolean registerRecoveredSession(
            TradeSession session
    ) {
        if (session == null || !session.isActive()) {
            return false;
        }

        UUID one = session.getPlayerOne();
        UUID two = session.getPlayerTwo();

        if (sessions.containsKey(one) || sessions.containsKey(two)) {
            return false;
        }

        sessions.put(one, session);
        sessions.put(two, session);

        return true;
    }

    public boolean sendRequest(UUID sender, UUID target) {
        if (sender == null || target == null || sender.equals(target)) {
            return false;
        }

        if (isInTrade(sender) || isInTrade(target)) {
            return false;
        }

        if (areRequestsDisabled(target)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long cooldown = requestCooldowns.getOrDefault(sender, 0L);

        if (now < cooldown) {
            return false;
        }

        if (requests.containsKey(target)) {
            return false;
        }

        requests.put(target, new TradeRequest(sender, target));
        requestCooldowns.put(sender, now + 5000L);

        return true;
    }

    public TradeRequest getRequest(UUID target) {
        return getRequest(target, 60);
    }

    public TradeRequest getRequest(UUID target, int expireSeconds) {
        if (target == null) {
            return null;
        }

        TradeRequest request = requests.get(target);

        if (request == null) {
            return null;
        }

        if (request.isExpired(expireSeconds)) {
            requests.remove(target, request);
            return null;
        }

        return request;
    }

    public synchronized TradeSession acceptRequest(UUID target) {
        return acceptRequest(target, 60);
    }

    public synchronized TradeSession acceptRequest(
            UUID target,
            int expireSeconds
    ) {
        TradeRequest request = getRequest(target, expireSeconds);

        if (request == null) {
            return null;
        }

        UUID sender = request.getSender();

        if (isInTrade(sender) || isInTrade(target)) {
            requests.remove(target, request);
            return null;
        }

        TradeSession session = new TradeSession(sender, target);

        if (storage != null && !saveSession(session)) {
            return null;
        }

        requests.remove(target, request);

        sessions.put(sender, session);
        sessions.put(target, session);

        return session;
    }

    public void denyRequest(UUID target) {
        if (target != null) {
            requests.remove(target);
        }
    }

    public void removeRequests(UUID playerId) {
        if (playerId == null) {
            return;
        }

        requests.remove(playerId);

        requests.entrySet().removeIf(entry ->
                entry.getValue().getSender().equals(playerId)
        );
    }

    public TradeSession getSession(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        TradeSession session = sessions.get(playerId);

        if (session != null && !session.isActive()) {
            removeSession(session);
            return null;
        }

        return session;
    }

    public boolean isInTrade(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        TradeSession session = sessions.get(playerId);
        return session != null && session.isActive();
    }

    public Collection<TradeSession> getActiveSessions() {
        return new ArrayList<>(
                sessions.values().stream()
                        .filter(TradeSession::isActive)
                        .distinct()
                        .toList()
        );
    }

    public synchronized void removeSession(TradeSession session) {
        if (session == null) {
            return;
        }

        sessions.remove(session.getPlayerOne(), session);
        sessions.remove(session.getPlayerTwo(), session);
    }

    public synchronized void completeSession(TradeSession session) {
        if (session == null) {
            return;
        }

        session.setCompleted(true);
        removeSession(session);
    }

    public void clearRequests() {
        requests.clear();
        requestCooldowns.clear();
    }

    public void setRequestsDisabled(UUID playerId, boolean disabled) {
        if (playerId == null) {
            return;
        }

        if (disabled) {
            disabledRequests.put(playerId, true);
        } else {
            disabledRequests.remove(playerId);
        }
    }

    public boolean areRequestsDisabled(UUID playerId) {
        return playerId != null
                && Boolean.TRUE.equals(disabledRequests.get(playerId));
    }

    public void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }

        removeRequests(playerId);
        requestCooldowns.remove(playerId);
    }
}
