/*
This class provides the peer information that is obtained from PeerInfo.cfg
 */

import java.util.concurrent.atomic.AtomicLong;

public class peerInfo {

	private String peerID;
    private String peerAddress;
    private int peerPort;
    private boolean containsFile;
    private boolean choked = true; 

    private AtomicLong downloadedBytes = new AtomicLong(0);
    private long lastMeasuredTime = System.currentTimeMillis();

	public peerInfo(String peerID, String peerAddress, String peerPort, String containsFile) {
		setPeerID(peerID);
		setPeerAddress(peerAddress);
		setPeerPort(peerPort);
		setContainsFile(containsFile);
	}

	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}

	public void setPeerAddress(String peerAddress) {
		this.peerAddress = peerAddress;
	}

	public void setPeerPort(String peerPort) {
		this.peerPort = Integer.parseInt(peerPort);
	}

	public void setContainsFile(String containsFile) {
		int contains = Integer.parseInt(containsFile);
		this.containsFile = contains != 0;
	}

	public String getPeerID() {
		return peerID;
	}

	public String getPeerAddress() {
		return peerAddress;
	}

	public int getPeerPort() {
		return peerPort;
	}

	public boolean getContainsFile() {
		return containsFile;
	}

	public void choke() {
        this.choked = true;
    }

    public void unchoke() {
        this.choked = false;
    }

    public boolean isChoked() {
        return choked;
    }

	// Method to update the downloaded bytes
	public void updateDownloadedBytes(long bytes) {
		downloadedBytes.addAndGet(bytes);
	}

	// Method to get the download speed in bytes per second
	public double getDownloadSpeed() {
		long currentTime = System.currentTimeMillis();
		long timeDiff = currentTime - lastMeasuredTime;
		if (timeDiff == 0)
			return 0.0;
		double speed = downloadedBytes.get() / ((double) timeDiff / 1000);
		downloadedBytes.set(0);
		lastMeasuredTime = currentTime;
		return speed;
	}

}
