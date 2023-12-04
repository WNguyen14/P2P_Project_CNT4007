import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import logging.PeerEventLogger;
import errorhandling.P2PFileSharingException;

/**
 * InterestManager class manages the interest status of peers in the P2P network.
 * It tracks which peers are interested in which pieces of the file.
 */
public class InterestManager {
    private Map<Integer, Set<Integer>> interestedPeers;

    /**
     * Constructor for InterestManager.
     */
    public InterestManager() {
        interestedPeers = new ConcurrentHashMap<>();
    }

    /**
     * Adds a peer's interest in a specific piece.
     *
     * @param peerId The ID of the interested peer.
     * @param pieceIndex The index of the piece the peer is interested in.
     */
    public void addInterestedPeer(int peerId, int pieceIndex) {
        interestedPeers.computeIfAbsent(peerId, k -> new HashSet<>()).add(pieceIndex);
        // Log the event of adding an interested peer
        PeerEventLogger.receivedInterestedMessage(peerId, pieceIndex);
    }

    /**
     * Removes a peer's interest in a specific piece.
     *
     * @param peerId The ID of the peer.
     * @param pieceIndex The index of the piece the peer is no longer interested in.
     */
    public void removeInterestedPeer(int peerId, int pieceIndex) {
        interestedPeers.computeIfPresent(peerId, (k, v) -> {
            v.remove(pieceIndex);
            // Log the event of removing an interested peer
            PeerEventLogger.receivedNotInterestedMessage(peerId, pieceIndex);
            return v.isEmpty() ? null : v;
        });
    }

    /**
     * Removes all pieces a peer is interested in.
     *
     * @param peerId The ID of the peer.
     */
    public void removeAllInterestedPieces(int peerId) {
        interestedPeers.remove(peerId);
        // Log removal of all interested pieces for a peer
        PeerEventLogger.receivedNotInterestedMessage(peerId, -1);// -1 denotes unknown neighbor
    }

    /**
     * Checks if there are any peers interested in a specific piece.
     *
     * @param pieceIndex The index of the piece.
     * @return true if any peer is interested in the piece, false otherwise.
     */
    public boolean hasInterestedPeers(int pieceIndex) {
        return interestedPeers.values().stream().anyMatch(set -> set.contains(pieceIndex));
    }

    /**
     * Checks if a specific peer is interested in any piece.
     *
     * @param peerId The ID of the peer.
     * @return true if the peer is interested in at least one piece, false otherwise.
     */
    public boolean isPeerInterested(int peerId) {
        return interestedPeers.containsKey(peerId) && !interestedPeers.get(peerId).isEmpty();
    }

    /**
     * Retrieves a set of peer IDs that are interested in any of the pieces of the specified peer.
     *
     * @param peerId The ID of the peer whose interested peers are to be found.
     * @return A set of peer IDs interested in the peer's pieces.
     */
    public Set<Integer> getPeersInterestedIn(int peerId) {
        return interestedPeers.entrySet().stream()
                .filter(entry -> entry.getValue().contains(peerId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
