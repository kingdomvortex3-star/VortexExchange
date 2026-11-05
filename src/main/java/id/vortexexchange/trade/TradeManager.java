
package id.vortexexchange.trade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final Map<UUID, TradeRequest> requests =
            new ConcurrentHashMap<>();

    private final Map<UUID, TradeSession> sessions =
            new ConcurrentHashMap<>();

    public boolean sendRequest(UUID sender, UUID target) {
        if (sender == null || target == null
                || sender.equals(target)) {
            return false;
        }

        if (isInTrade(sender) || isInTrade(target)) {
            return false;
        }

        if (getRequest(target) != null) {
            return false;
        }

        requests.put(target, new TradeRequest(sender, target));
        return true;
    }

    public TradeRequest getRequest(UUID target) {
        TradeRequest request = requests.get(target);

        if (request == null) {
            return null;
        }

        if (request.isExpired(60)) {
            requests.remove(target, request);
            return null;
        }

        return request;
    }

    public synchronized TradeSession acceptRequest(UUID target) {
        TradeRequest request = getRequest(target);

        if (request == null) {
            return null;
        }

        UUID sender = request.getSender();

        if (isInTrade(sender) || isInTrade(target)) {
            requests.remove(target, request);
            return null;
        }

        TradeSession session = new TradeSession(sender, target);

        sessions.put(sender, session);
        sessions.put(target, session);

        requests.remove(target, request);

        return session;
    }

    public boolean denyRequest(UUID target) {
        return requests.remove(target) != null;
    }

    public void removeRequests(UUID player) {
        requests.remove(player);

        requests.entrySet().removeIf(entry ->
                entry.getValue().getSender().equals(player)
        );
    }

    public TradeSession getSession(UUID player) {
        return sessions.get(player);
    }

    public boolean isInTrade(UUID player) {
        return sessions.containsKey(player);
    }

    public List<TradeSession> getActiveSessions() {
        Set<TradeSession> unique = new HashSet<>(sessions.values());

        List<TradeSession> result = new ArrayList<>();

        for (TradeSession session : unique) {
            if (session != null && session.isActive()) {
                result.add(session);
            }
        }

        return result;
    }

    public synchronized void removeSession(TradeSession session) {
        if (session == null || !session.isActive()) {
            return;
        }

        session.setCancelled(true);

        sessions.remove(session.getPlayerOne(), session);
        sessions.remove(session.getPlayerTwo(), session);
    }

    public synchronized void completeSession(TradeSession session) {
        if (session == null || !session.isActive()) {
            return;
        }

        session.setCompleted(true);

        sessions.remove(session.getPlayerOne(), session);
        sessions.remove(session.getPlayerTwo(), session);
    }

    public void clearRequests() {
        requests.clear();
    }
}
