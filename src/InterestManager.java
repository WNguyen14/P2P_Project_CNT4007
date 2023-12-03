import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterestManager {
    private final Map<Integer, Set<Integer>> interestedPeers;

    public InterestManager() {
        interestedPeers = new ConcurrentHashMap<>();
    }

    public void addInterestedPeer(int peerId, int pieceIndex) {
        interestedPeers.computeIfAbsent(peerId, k -> new HashSet<>()).add(pieceIndex);
    }

    public void removeInterestedPeer(int peerId, int pieceIndex) {
        interestedPeers.computeIfPresent(peerId, (k, v) -> {
            v.remove(pieceIndex);
            return v.isEmpty() ? null : v;
        });
    }

    public void removeAllInterestedPieces(int peerId) {
        interestedPeers.remove(peerId);
    }

    public boolean hasInterestedPeers(int pieceIndex) {
        return interestedPeers.values().stream().anyMatch(set -> set.contains(pieceIndex));
    }

    // Additional methods can be added here for managing the interest map
    // Example: Method to check if a peer is interested in any piece
    public boolean isPeerInterested(int peerId) {
        return interestedPeers.containsKey(peerId) && !interestedPeers.get(peerId).isEmpty();
    }
}
