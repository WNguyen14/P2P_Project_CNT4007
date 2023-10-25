Timer timer = new Timer();
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class chokehandler {
    //initialize variables
    private Set<String> unchoked = new HashSet<String> (); 
    private ScheduledFuture<?> job = null;
    private ScheduledExecutorService timer = null;
    private int unchokingInterval;
    private int numPreferred;
    private peerProcess optimisticUnchoked;

    public void start{
        this.job = this.timer.schedule(chokeCheck, unchokingInterval, unchokingInterval);
    }
}

TimerTask chokeCheck = new TimerTask() {
    @Override
    public void run()
    {
        //TODO: get the set of unchoked peers
        Set<String> unchokedlist;

        //initialize a new set to fill after finding unchoked peers
        Set<String> newset = new HashSet<String> (); 
        
        //TODO: grab list of interested peers
        Set<String> interested = new HashSet<String> (); 

        //iterate through the interested peers to find new preferred
        if(!interested.isEmpty())
        {
            for(int i = 0; i<numPreferred; i++)
            {
                int maxdownload = 0;
                String bestPeer = "";
                for(String s: unchokedlist)
                {
                    //calculate the download rate of peer S
                    int download = 0;
                    //find the max download rate
                    if (download > maxdownload)
                    {
                        maxdownload = download;
                        bestPeer = s;
                    }

                }
                //TODO: send the unchoke message to bestPeer
            }


        }
        //reset the unchoked list
        else
        {
            for(strint p: unchokedlist)
            {
                //send the message that the peer is choked
            }            
        }

    }
}