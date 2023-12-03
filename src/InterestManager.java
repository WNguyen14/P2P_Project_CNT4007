import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InterestManager {
    private Map<Integer, Set<Integer>> interestedPeers;

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

    
    public boolean isPeerInterested(int peerId) {
        return interestedPeers.containsKey(peerId) && !interestedPeers.get(peerId).isEmpty();
    }

     // This method returns a set of peer IDs that are interested in any of the pieces
    // of the peer with the given peerId
    public Set<Integer> getPeersInterestedIn(int peerId) {
        // We check each entry in the map to see if it contains the pieceIndex
        return interestedPeers.entrySet().stream()
                .filter(entry -> entry.getValue().contains(peerId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }


    
    
}
