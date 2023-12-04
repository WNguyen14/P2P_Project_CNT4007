import logging.ConnectionEventLogger;
import logging.PeerEventLogger;
import errorhandling.P2PFileSharingException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The peerInfo class stores and manages information for each peer in the P2P network.
 * It holds peer-specific data such as ID, address, port, file possession status, and download statistics.
 */
public class peerInfo {

    private String peerID;
    private String peerAddress;
    private int peerPort;
    private boolean containsFile;
    private boolean choked = true; 

    private AtomicLong downloadedBytes = new AtomicLong(0);
    private long lastMeasuredTime = System.currentTimeMillis();

    /**
     * Constructs a peerInfo object with given parameters.
     *
     * @param peerID ID of the peer.
     * @param peerAddress IP address or hostname of the peer.
     * @param peerPort Port number on which the peer listens.
     * @param containsFile Indicates whether the peer has the complete file.
     */
    public peerInfo(String peerID, String peerAddress, String peerPort, String containsFile) {
        setPeerID(peerID);
        setPeerAddress(peerAddress);
        setPeerPort(peerPort);
        setContainsFile(containsFile);
    }

    // Setter and getter methods for peer information
    public void setPeerID(String peerID) { this.peerID = peerID; }
    public void setPeerAddress(String peerAddress) { this.peerAddress = peerAddress; }
    public void setPeerPort(String peerPort) { this.peerPort = Integer.parseInt(peerPort); }
    public void setContainsFile(String containsFile) {
        this.containsFile = Integer.parseInt(containsFile) != 0;
    }
    public String getPeerID() { return peerID; }
    public String getPeerAddress() { return peerAddress; }
    public int getPeerPort() { return peerPort; }
    public boolean getContainsFile() { return containsFile; }

    // Methods to manage choking status of the peer
    public void choke() {
        this.choked = true;
        PeerEventLogger.peerChoked(Integer.parseInt(peerID), -1); // -1 denotes unknown neighbor
    }
    public void unchoke() {
        this.choked = false;
        PeerEventLogger.peerUnchoked(Integer.parseInt(peerID), -1); // -1 denotes unknown neighbor
    }
    public boolean isChoked() { return choked; }

    /**
     * Updates the downloaded bytes for the peer.
     * 
     * @param bytes The number of bytes downloaded.
     */
    public void updateDownloadedBytes(long bytes) {
        downloadedBytes.addAndGet(bytes);
    }

    /**
     * Calculates the download speed of the peer in bytes per second.
     * 
     * @return The download speed in bytes per second.
     */
    public double getDownloadSpeed() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastMeasuredTime;
        if (timeDiff == 0) return 0.0;
        double speed = downloadedBytes.get() / ((double) timeDiff / 1000);
        downloadedBytes.set(0);
        lastMeasuredTime = currentTime;
        return speed;
    }

}
