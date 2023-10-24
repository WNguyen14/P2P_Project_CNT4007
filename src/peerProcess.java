import java.util.*;

public class peerProcess {

	private String myPeerID;
	private Config configInfo;
	private HashMap<String, peerInfo> allPeerInfo;

	private HashMap<String, BitSet> pieceAvailability;
	private peerInfo myPeerInfo;

	public peerProcess(String myPeerID) {
		this.myPeerID = myPeerID;
		this.configInfo = new Config("Common.cfg");
		this.allPeerInfo = makePeerInfo("PeerInfo.cfg");
		this.myPeerInfo =  allPeerInfo.get(myPeerID);
	}

	public HashMap<String, peerInfo> makePeerInfo(String fileName) {
		Scanner in = new Scanner(fileName);
		HashMap<String, peerInfo> peersMap = null;

		while (in.hasNextLine()){
			String[] line = in.nextLine().split(" ");
			String peerID = line[0];
			String peerAddress = line[1];
			String peerPort = line[2];
			String containsFile = line[3];
			peerInfo newPeer = new peerInfo(peerID, peerAddress, peerPort, containsFile);
			peersMap.put(peerID, newPeer);
		}
		in.close();
		return peersMap;
	}

	public void initPieces() {
		for (Map.Entry<String, peerInfo> entry : allPeerInfo.entrySet()) {
			BitSet available = new BitSet(getNumPieces());
			if (this.allPeerInfo.get(entry.getKey()).getContainsFile()) {
				available.set(0, getNumPieces());
				this.pieceAvailability.put(entry.getKey(), available);
			}
		}
	}

	public int getNumPieces() {
		int l = configInfo.getFileSize() / configInfo.getPieceSize();
		if (configInfo.getFileSize() % configInfo.getPieceSize() != 0) {
			l +=1;
		}
		return l;
	}

}
