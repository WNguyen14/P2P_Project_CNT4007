/*
entry point to start the peer processes
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class peerProcess {

    private int myPeerID;
    private Config configInfo;
    private HashMap<String, peerInfo> allPeerInfo;
    private HashMap<String, BitSet> pieceAvailability;
    private peerInfo myPeerInfo;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    private static final Map<Socket, Integer> socketToPeerIdMap = new ConcurrentHashMap<>();


    private InterestManager interestManager = new InterestManager();

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: java peerProcess <peerID>");
                return;
            }
            int peerID = Integer.parseInt(args[0]);
            new peerProcess(peerID).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public peerProcess(int myPeerID) throws FileNotFoundException {
        this.myPeerID = myPeerID;
        this.configInfo = new Config("Common.cfg");
        this.allPeerInfo = makePeerInfo("PeerInfo.cfg");
        this.myPeerInfo = allPeerInfo.get(Integer.toString(myPeerID));
        this.pieceAvailability = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();

    }

    private void start() throws IOException {
        System.out.println("Peer " + myPeerID + " starting...");
        initPieces();

        // Start listening for incoming connections in a new thread.
        startServer();

        // Connect to other peers that have started before this peer.
        connectToPreviousPeers();

        System.out.println("Successfully initialized peer " + myPeerID);
    }

    private void startServer() throws IOException {
        int myPort = this.myPeerInfo.getPeerPort();
        this.serverSocket = new ServerSocket(myPort);
        executor.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
    
                    // Determine the peer ID after the handshake
                    int peerId = determinePeerId(clientSocket);
                    // Store peerId as an Integer in socketToPeerIdMap
                    socketToPeerIdMap.put(clientSocket, peerId);
    
                    FileManager fm = new FileManager(
                            configInfo.getFileSize(),
                            configInfo.getPieceSize(),
                            configInfo.getConfigFileName(),
                            myPeerInfo.getContainsFile());
    
                    // Pass the interestManager instance to the PeerHandler
                    executor.submit(new PeerHandler(clientSocket, fm, interestManager, pieceAvailability));
                } catch (IOException e) {
                    // Error handling remains unchanged
                }
            }
        });
    }

    private int determinePeerId(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] handshake = new byte[32]; // The handshake message is 32 bytes long.
        in.readFully(handshake); // Read the handshake message.

        // Extract the peer ID from the handshake message as an integer
        return ByteBuffer.wrap(Arrays.copyOfRange(handshake, 28, 32)).getInt();
    }
    // In peerProcess.java
private void connectToPreviousPeers() {
    // Connect to peers with a lower peer ID (which means they started earlier).
    allPeerInfo.forEach((peerID, info) -> {
        int currentPeerID = Integer.parseInt(peerID);
        if (currentPeerID < myPeerID) {
            try {
                Socket peerSocket = new Socket(info.getPeerAddress(), info.getPeerPort());
                
                // Perform handshake
                handshake hs = new handshake(myPeerID);
                DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
                DataInputStream in = new DataInputStream(peerSocket.getInputStream());
                
                out.write(hs.createHandshake());
                out.flush();
                
                // Read handshake response
                byte[] response = new byte[32];
                in.readFully(response);
                
                // Validate handshake response
                int receivedPeerID = ByteBuffer.wrap(Arrays.copyOfRange(response, 28, 32)).getInt();
                if (receivedPeerID != currentPeerID) {
                    throw new IOException("Handshake response from incorrect peer: Expected " + currentPeerID + " but received " + receivedPeerID);
                }
                
                // Store the handshake information
                socketToPeerIdMap.put(peerSocket, receivedPeerID);
                
                // Pass the interestManager instance to the PeerHandler
                FileManager fm = new FileManager(configInfo.getFileSize(), configInfo.getPieceSize(), configInfo.getConfigFileName(), myPeerInfo.getContainsFile());
                PeerHandler peerHandler = new PeerHandler(peerSocket, fm, interestManager, pieceAvailability);
                
                // Start a new thread to handle this peer connection
                executor.submit(peerHandler);
                
            } catch (IOException e) {
                System.err.println("Error connecting to peer " + currentPeerID + ": " + e.getMessage());
            }
        }
    });
}


    private HashMap<String, peerInfo> makePeerInfo(String fileName) throws FileNotFoundException {
        Scanner in = new Scanner(new FileReader(fileName));
        HashMap<String, peerInfo> peersMap = new HashMap<>();

        while (in.hasNextLine()) {
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

    private void initPieces() {
        int numPieces = getNumPieces();
        for (Map.Entry<String, peerInfo> entry : allPeerInfo.entrySet()) {
            BitSet available = new BitSet(numPieces);
            if (entry.getValue().getContainsFile()) {
                available.set(0, numPieces);
            }
            this.pieceAvailability.put(entry.getKey(), available);
        }
    }

    public BitSet getPeerBitfield(String peerId) {
        return pieceAvailability.get(peerId);
    }
    

    private int getNumPieces() {
        int l = configInfo.getFileSize() / configInfo.getPieceSize();
        if (configInfo.getFileSize() % configInfo.getPieceSize() != 0) {
            l += 1;
        }
        return l;
    }
}
