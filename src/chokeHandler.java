import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import logging.ConnectionEventLogger;
import logging.PeerEventLogger;

public class chokeHandler {
    private List<String> unchoked;
    private List<String> interestedNeighbors;

    private final int unchokingInterval;
    private final int optUnchokingInterval;
    private final int numPreferred;

    private final HashMap<String, peerInfo> allPeerInfo;
    private final InterestManager interestManager;

    public chokeHandler(int unchokingInterval, int optUnchokingInterval, int numPreferred, HashMap<String, peerInfo> allPeerInfo, InterestManager interestManager) {
        this.unchoked = new ArrayList<>();
        this.interestedNeighbors = new ArrayList<>();
        this.unchokingInterval = unchokingInterval;
        this.optUnchokingInterval = optUnchokingInterval;
        this.numPreferred = numPreferred;
        this.allPeerInfo = allPeerInfo;
        this.interestManager = interestManager;
    }

    public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // Regular unchoking
        Runnable cncRunnable = this::chokeUnchoke;
        scheduler.scheduleAtFixedRate(cncRunnable, 0, unchokingInterval, TimeUnit.SECONDS);

        // Optimistic unchoking
        Runnable optRunnable = this::optUnchoke;
        scheduler.scheduleAtFixedRate(optRunnable, 0, optUnchokingInterval, TimeUnit.SECONDS);
    }

    private void chokeUnchoke() {
        // Iterate through all peers
        for (Map.Entry<String, peerInfo> entry : allPeerInfo.entrySet()) {
            String peerId = entry.getKey();
            peerInfo peer = entry.getValue();

            // Get interested peers for this peer
            Set<Integer> interestedPeers = interestManager.getPeersInterestedIn(Integer.parseInt(peerId));
            interestedNeighbors = interestedPeers.stream()
                                                 .map(Object::toString)
                                                 .collect(Collectors.toList());

            // Sort interested neighbors based on download speed
            interestedNeighbors.sort(Comparator.comparing(pId -> allPeerInfo.get(pId).getDownloadSpeed()).reversed());

            // Pick top 'numPreferred' neighbors
            List<String> preferredNeighbors = new ArrayList<>();
            if (interestedNeighbors.size() > numPreferred) {
                preferredNeighbors.addAll(interestedNeighbors.subList(0, numPreferred));
            } else {
                preferredNeighbors.addAll(interestedNeighbors);
            }

            // Log preferred neighbors
            PeerEventLogger.preferredNeighborsChanged(Integer.parseInt(peerId), String.join(",", preferredNeighbors));

            // Unchoke preferred neighbors and choke others
            updateChokingStatus(peerId, preferredNeighbors);
        }
    }

    private void updateChokingStatus(String peerId, List<String> preferredNeighbors) {
        for (String neighbor : interestedNeighbors) {
            peerInfo neighborInfo = allPeerInfo.get(neighbor);

            if (preferredNeighbors.contains(neighbor)) {
                if (!unchoked.contains(neighbor)) {
                    neighborInfo.unchoke();
                    unchoked.add(neighbor);
                    PeerEventLogger.peerUnchoked(Integer.parseInt(peerId), Integer.parseInt(neighbor));
                }
            } else {
                if (unchoked.contains(neighbor)) {
                    neighborInfo.choke();
                    unchoked.remove(neighbor);
                    PeerEventLogger.peerChoked(Integer.parseInt(peerId), Integer.parseInt(neighbor));
                }
            }
        }
    }

    private void optUnchoke() {
        if (interestedNeighbors.isEmpty()) {
            return; // No interested neighbors to optimistically unchoke
        }

        // Randomly select an optimistically unchoked neighbor
        Random rand = new Random();
        String optUnchokedNeighbor = interestedNeighbors.get(rand.nextInt(interestedNeighbors.size()));

        if (!unchoked.contains(optUnchokedNeighbor)) {
            allPeerInfo.get(optUnchokedNeighbor).unchoke();
            unchoked.add(optUnchokedNeighbor);
            PeerEventLogger.optimisticNeighborChanged(Integer.parseInt(optUnchokedNeighbor), Integer.parseInt(optUnchokedNeighbor));
        }
    }
}
