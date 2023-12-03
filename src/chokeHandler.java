import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import logging.ConnectionEventLogger;
import logging.PeerEventLogger;


 public class chokeHandler {
     //initialize variables
     private List<String> unchoked;
     public List<String> interestedNeighbors;

     private int unchokingInterval;
     private int optUnchokingInterval;
     private int numPreferred;
     //make a hashmap and grab peerInfo from peerProcess
     private HashMap<String, peerInfo> allPeerInfo;
     
     private InterestManager interestManager;

     public chokeHandler(int Interval, int pref, HashMap<String, peerInfo> a, InterestManager interestManager) {
        unchokingInterval = Interval;
        numPreferred = pref;
        allPeerInfo = a;
        this.interestManager = interestManager; 
    }
     public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        //schedule to run the chokeUnchoke function every unchokingInterval seconds
        Runnable cncRunnable = new Runnable() {
            public void run() {
                chokeUnchoke(allPeerInfo);
            }
        };
        Runnable optRunnable = new Runnable() {
            public void run() {
                optUnchoke(allPeerInfo);
            }
        };

        scheduler.scheduleAtFixedRate(cncRunnable, 0, unchokingInterval, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(optRunnable, 0, optUnchokingInterval, TimeUnit.SECONDS);
    }

    private void chokeUnchoke(HashMap<String, peerInfo> allPeerInfo) {
        //list all peers in allPeerInfo 
        for (Map.Entry<String, peerInfo> entry : allPeerInfo.entrySet()) {
            String key = entry.getKey();
            peerInfo value = entry.getValue();
            //list interested neighbors using interestmanager
            Set<Integer> interestedPeers = interestManager.getPeersInterestedIn(Integer.parseInt(value.getPeerID()));
            interestedNeighbors = interestedPeers.stream()
                                    .map(Object::toString)
                                    .collect(Collectors.toList());

            Collections.sort(interestedNeighbors, Comparator.comparing(peerId -> allPeerInfo.get(peerId).getDownloadSpeed()).reversed());
            List<String> preferredNeighbors = new ArrayList<>(interestedNeighbors.subList(0, Math.min(numPreferred, interestedNeighbors.size())));
            //log preferred neighbors
            PeerEventLogger.preferredNeighborsChanged(Integer.parseInt(value.getPeerID()), String.join(",", preferredNeighbors));
            //unchoke all preferred neighbors
            for (String neighbor : preferredNeighbors) {
                if (!unchoked.contains(neighbor)) {
                    allPeerInfo.get(neighbor).unchoke();
                    unchoked.add(neighbor);
                    //log unchoked neighbor
                    PeerEventLogger.optimisticNeighborChanged(Integer.parseInt(allPeerInfo.get(neighbor).getPeerID()), Integer.parseInt(allPeerInfo.get(neighbor).getPeerID()));
                }
            }
            //choke all other neighbors
            for (String neighbor : interestedNeighbors) {
                if (!preferredNeighbors.contains(neighbor)) {
                    if (unchoked.contains(neighbor)) {
                        unchoked.remove(neighbor);
                    }
                    allPeerInfo.get(neighbor).choke();
                    //log choked neighbor
                    PeerEventLogger.peerChoked(Integer.parseInt(value.getPeerID()), Integer.parseInt(neighbor));
                }
            }

        }
        
    }
    private void optUnchoke(HashMap<String, peerInfo> allPeerInfo){
            //determine optomistically unchoked neighbor by choosing randomly from the interested neighbors
            Random rand = new Random();
            //get a random interested neighbor
            String optUnchokedNeighbor = interestedNeighbors.get(rand.nextInt(interestedNeighbors.size()));
            //if the optomistically unchoked neighbor is not already unchoked, unchoke them
            if (!unchoked.contains(optUnchokedNeighbor)) {
                allPeerInfo.get(optUnchokedNeighbor).unchoke();
                unchoked.add(optUnchokedNeighbor);
                //log the change in optomistically unchoked neighbor
                PeerEventLogger.optimisticNeighborChanged(Integer.parseInt(allPeerInfo.get(optUnchokedNeighbor).getPeerID()), Integer.parseInt(allPeerInfo.get(optUnchokedNeighbor).getPeerID()));
            }
    }
     
 }