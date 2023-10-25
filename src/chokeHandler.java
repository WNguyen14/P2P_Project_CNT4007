 import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
 import java.util.concurrent.TimeUnit;

 public class chokeHandler {
     //initialize variables
     private Set<String> unchoked = new HashSet<String>();
     private ScheduledFuture<?> job = null;
     private ScheduledExecutorService timer = null;
     private int unchokingInterval;
     private int numPreferred;
     private peerProcess optimisticUnchoked;

     public void start() {
         this.job = this.timer.scheduleAtFixedRate(chokeCheck, unchokingInterval, unchokingInterval, TimeUnit.SECONDS);
     }


     TimerTask chokeCheck = new TimerTask() {
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
     };
 }