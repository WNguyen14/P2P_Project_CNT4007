import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

 public class chokeHandler {
     //initialize variables
     private Set<String> unchoked = new HashSet<String>();
     private ScheduledFuture<?> job = null;
     private ScheduledExecutorService timer = null;

     private int unchokingInterval;
     private int optUnchokingInterval;
     private int numPreferred;
     //make a hashmap and grab peerInfo from peerProcess
     private HashMap<String, peerInfo> allPeerInfo;
     
     public chokeHandler(int Interval, int pref){
        unchokingInterval = Interval;
        numPreferred = pref; 
        allPeerInfo = peerProcess.makePeerInfo();
     }

     public void start() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        //schedule to run the chokeUnchoke function every unchokingInterval seconds
        scheduler.scheduleAtFixedRate(this::chokeUnchoke, 0, unchokingInterval, TimeUnit.SECONDS);
    }

    private void chokeUnchoke() {
        //list all peers in allPeerInfo 
        for (Map.Entry<String, peerInfo> entry : allPeerInfo.entrySet()) {
            String key = entry.getKey();
            peerInfo value = entry.getValue();
            //list interested neighbors using interestmanager
            Set<String> interestedNeighbors = InterestManager.getPeersInterestedIn(value.getPeerID());
            //out of the interested neighbors, find the one with the fastest download speed
            Collections.sort(interestedNeighbors, Comparator.comparingDouble(0).reversed());
            List<String> preferredNeighbors = interestedNeighbors.subList(0, numPreferred);
            //unchoke all preferred neighbors
            for (String neighbor : preferredNeighbors) {
                if (!unchoked.contains(neighbor)) {
                    allPeerInfo[neighbor].unchoke();
                    unchoked.add(neighbor);
                }
            }
            //choke all other neighbors
            for (String neighbor : interestedNeighbors) {
                if (!preferredNeighbors.contains(neighbor)) {
                    if (unchoked.contains(neighbor)) {
                        unchoked.remove(neighbor);
                    }
                    allPeerInfo[neighbor].choke();
                }
            }
            //determine optomistically unchoked neighbor by choosing randomly from the interested neighbors
            Random rand = new Random();
            String optUnchokedNeighbor = interestedNeighbors.get(rand.nextInt(interestedNeighbors.size()));
            //if the optomistically unchoked neighbor is not already unchoked, unchoke them
            if (!unchoked.contains(optUnchokedNeighbor)) {
                allPeerInfo[optUnchokedNeighbor].unchoke();
                unchoked.add(optUnchokedNeighbor);
            }
        }
        
    }
     
 }