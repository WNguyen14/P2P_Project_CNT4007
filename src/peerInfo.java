/*
This class provides the peer information that is obtained from PeerInfo.cfg
 */

public class peerInfo {

	private String peerID;
	private String peerAddress;
	private int peerPort;
	private boolean containsFile;

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
}
