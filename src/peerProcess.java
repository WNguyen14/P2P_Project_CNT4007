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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.DataInputStream;

public class peerProcess {

    private String myPeerID;
    private Config configInfo;
    private HashMap<String, peerInfo> allPeerInfo;
    private HashMap<String, BitSet> pieceAvailability;
    private peerInfo myPeerInfo;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    private static final Map<Socket, String> socketToPeerIdMap = new ConcurrentHashMap<>();


    private InterestManager interestManager = new InterestManager();

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: java peerProcess <peerID>");
                return;
            }
            new peerProcess(args[0]).start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public peerProcess(String myPeerID) throws FileNotFoundException {
        this.myPeerID = myPeerID;
        this.configInfo = new Config("Common.cfg");
        this.allPeerInfo = makePeerInfo("PeerInfo.cfg");
        this.myPeerInfo = allPeerInfo.get(myPeerID);
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
    
                    // Here you can add the logic to determine the peer ID after the handshake
                    // For now, let's assume you have a method that does that
                    String peerId = determinePeerId(clientSocket);
                    socketToPeerIdMap.put(clientSocket, peerId);
    
                    FileManager fm = new FileManager(
                            configInfo.getFileSize(),
                            configInfo.getPieceSize(),
                            configInfo.getConfigFileName(),
                            myPeerInfo.getContainsFile());

                    // Pass the interestManager instance to the PeerHandler
                    executor.submit(new PeerHandler(clientSocket, fm, interestManager));
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        System.out.println("Server socket is closed, stopping the server.");
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private String determinePeerId(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] handshake = new byte[32]; // According to your protocol, the handshake message is 32 bytes long.
        in.readFully(handshake); // Read the handshake message.
    
        // Extract the peer ID from the handshake message.
        // The peer ID is the last 4 bytes of the handshake message.
        String peerId = new String(Arrays.copyOfRange(handshake, 28, 32), StandardCharsets.UTF_8);
        return peerId;
    }
    
    private void connectToPreviousPeers() {
        // Connect to peers with a lower peer ID (which means they started earlier).
        allPeerInfo.forEach((peerID, info) -> {
            if (Integer.parseInt(peerID) < Integer.parseInt(myPeerID)) {
                try {
                    Socket peerSocket = new Socket(info.getPeerAddress(), info.getPeerPort());
                    // TODO: Handle the peer socket, e.g., handshake, bitfield exchange
                    FileManager fm = new FileManager(
                            configInfo.getFileSize(),
                            configInfo.getPieceSize(),
                            configInfo.getConfigFileName(),
                            myPeerInfo.getContainsFile());
                    executor.execute(new PeerHandler(peerSocket, fm, interestManager));

                } catch (IOException e) {
                    e.printStackTrace();
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

    private int getNumPieces() {
        int l = configInfo.getFileSize() / configInfo.getPieceSize();
        if (configInfo.getFileSize() % configInfo.getPieceSize() != 0) {
            l += 1;
        }
        return l;
    }
}
