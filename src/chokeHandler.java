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
     private int numPreferred;
     //make a hashmap and grab peerInfo from peerProcess
     private HashMap<String, peerInfo> allPeerInfo;
     
     public chokeHandler(int Interval, int pref){
        unchokingInterval = Interval;
        numPreferred = pref; 
        allPeerInfo = peerProcess.getPeerInfo();
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
            //make unchoked list of peers
            if (value.getInterested() && value.getChoked()) {
                unchoked.add(key);
            }
        }

        List<Neighbor> interestedNeighbors = peer.getInterestedNeighbors();
        Collections.sort(interestedNeighbors, Comparator.comparingDouble(Neighbor::getDownloadRate).reversed());

        List<Neighbor> preferredNeighbors = interestedNeighbors.subList(0, Math.min(preferredNeighbors, interestedNeighbors.size()));

        for (Neighbor neighbor : preferredNeighbors) {
            if (!neighbor.isUnchoked()) {
                peer.sendUnchokeMessage(neighbor);
                neighbor.setUnchoked(true);
            }
        }

        for (Neighbor neighbor : interestedNeighbors) {
            if (!preferredNeighbors.contains(neighbor) && neighbor.isUnchoked()) {
                peer.sendChokeMessage(neighbor);
                neighbor.setUnchoked(false);
            }
        }
    }
     /*TimerTask chokeCheck = new TimerTask() {
         @Override
         public void run() {
             //TODO: get the set of unchoked peers
             Set<String> unchokedlist = new HashSet<>();

             //initialize a new set to fill after finding unchoked peers
             Set<String> newset = new HashSet<>();

             //TODO: grab list of interested peers
             Set<String> interested = new HashSet<>();

             //iterate through the interested peers to find new preferred
             if (!interested.isEmpty()) {
                 for (int i = 0; i < numPreferred; i++) {
                     int maxdownload = 0;
                     String bestPeer = "";
                     for (String s : unchokedlist) {
                         //calculate the download rate of peer S
                         int download = 0;
                         //find the max download rate
                         if (download > maxdownload) {
                             maxdownload = download;
                             bestPeer = s;
                         }

                     }
                     //TODO: send the unchoke message to bestPeer
                 }


             }
             //reset the unchoked list
             else {
                 for (String p : unchokedlist) {
                     //send the message that the peer is choked
                 }
             }
         }
     };*/
 }