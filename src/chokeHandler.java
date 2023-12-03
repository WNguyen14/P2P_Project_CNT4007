import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

 public class chokeHandler {
     //initialize variables
     private List<String> unchoked;
     private ScheduledFuture<?> job = null;
     private ScheduledExecutorService timer = null;
     public List<String> interestedNeighbors;

     private int unchokingInterval;
     private int optUnchokingInterval;
     private int numPreferred;
     //make a hashmap and grab peerInfo from peerProcess
     private HashMap<String, peerInfo> allPeerInfo;
     
     public chokeHandler(int Interval, int pref, HashMap<String, peerInfo> a){
        unchokingInterval = Interval;
        numPreferred = pref; 
        allPeerInfo = a;
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
            interestedNeighbors = InterestManager.getPeersInterestedIn(Integer.valueOf(value.getPeerID()));
            //out of the interested neighbors, find the one with the fastest download speed
            Collections.sort(interestedNeighbors, Comparator.comparingInt(0).reversed());
            List<String> preferredNeighbors = interestedNeighbors.subList(0, numPreferred);
            //unchoke all preferred neighbors
            for (String neighbor : preferredNeighbors) {
                if (!unchoked.contains(neighbor)) {
                    allPeerInfo.get(neighbor).unchoke();
                    unchoked.add(neighbor);
                }
            }
            //choke all other neighbors
            for (String neighbor : interestedNeighbors) {
                if (!preferredNeighbors.contains(neighbor)) {
                    if (unchoked.contains(neighbor)) {
                        unchoked.remove(neighbor);
                    }
                    allPeerInfo.get(neighbor).choke();
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
            }
    }
     
 }