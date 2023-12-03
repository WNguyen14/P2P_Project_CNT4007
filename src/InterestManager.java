import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterestManager {
    private final Map<String, Set<Integer>> interestedPeers;

    public InterestManager() {
        interestedPeers = new ConcurrentHashMap<>();
    }

    public void addInterestedPeer(String peerId, int pieceIndex) {
        interestedPeers.computeIfAbsent(peerId, k -> new HashSet<>()).add(pieceIndex);
    }

    public void removeInterestedPeer(String peerId, int pieceIndex) {
        if (interestedPeers.containsKey(peerId)) {
            interestedPeers.get(peerId).remove(pieceIndex);
            if (interestedPeers.get(peerId).isEmpty()) {
                interestedPeers.remove(peerId);
            }
        }
    }

    public boolean hasInterestedPeers(int pieceIndex) {
        return interestedPeers.values().stream().anyMatch(set -> set.contains(pieceIndex));
    }
    
    // Add other methods as needed for your implementation...
}
