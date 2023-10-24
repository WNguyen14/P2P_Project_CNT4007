import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class peerProcess {

	private String myPeerID;
	private Config configInfo;
	private HashMap<String, peerInfo> allPeerInfo;
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


}
