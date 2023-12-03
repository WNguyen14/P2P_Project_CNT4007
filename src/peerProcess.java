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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import logging.ConnectionEventLogger;
import logging.Logger;


public class peerProcess {

    private int myPeerID;
    private Config configInfo;
    private HashMap<String, peerInfo> allPeerInfo;
    private HashMap<Integer, BitSet> pieceAvailability;
    private peerInfo myPeerInfo;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    // Map to keep track of peer IDs associated with sockets
    private static final Map<Socket, Integer> socketToPeerIdMap = new HashMap<>();

    private InterestManager interestManager = new InterestManager();

    // Main method to start the peer process
    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                System.out.println("Usage: java peerProcess <peerID>");
                return;
            }
            int peerID = Integer.parseInt(args[0]);
            new peerProcess(peerID).start();
        } catch (Exception e) {
            Logger.error("Error starting peer process: %s", e.getMessage());
            System.exit(1);
        }
    }

    // Constructor to initialize peer process
    public peerProcess(int myPeerID) throws FileNotFoundException {
        this.myPeerID = myPeerID;
        this.configInfo = new Config("Common.cfg");
        this.allPeerInfo = makePeerInfo("PeerInfo.cfg");
        this.myPeerInfo = allPeerInfo.get(Integer.toString(myPeerID));
        this.pieceAvailability = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    // Starts the peer process
    private void start() throws IOException {
        Logger.info("Peer %d starting...", myPeerID);
        initPieces();

        startServer();
        connectToPreviousPeers();

        Logger.info("Successfully initialized peer %d", myPeerID);
    }

    // Initializes the server to listen for incoming connections
    private void startServer() throws IOException {
        int myPort = this.myPeerInfo.getPeerPort();
        this.serverSocket = new ServerSocket(myPort);
        executor.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleClientSocket(clientSocket);
                } catch (IOException e) {
                    Logger.error("Error accepting client socket: %s", e.getMessage());
                }
            }
        });
    }

    // Handles a client socket connection
    private void handleClientSocket(Socket clientSocket) throws IOException {
        int peerId = determinePeerId(clientSocket);
        socketToPeerIdMap.put(clientSocket, peerId);

        FileManager fm = new FileManager(
                configInfo.getFileSize(),
                configInfo.getPieceSize(),
                configInfo.getConfigFileName(),
                myPeerInfo.getContainsFile());

        executor.submit(new PeerHandler(clientSocket, fm, interestManager, pieceAvailability));
        ConnectionEventLogger.peerConnected(myPeerID, peerId);
    }

    // Determines the peer ID from the handshake message
    private int determinePeerId(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] handshake = new byte[32];
        in.readFully(handshake);
        return ByteBuffer.wrap(Arrays.copyOfRange(handshake, 28, 32)).getInt();
    }

    // Connects to peers that started before this peer
    private void connectToPreviousPeers() {
        allPeerInfo.forEach((peerID, info) -> {
            int currentPeerID = Integer.parseInt(peerID);
            if (currentPeerID < myPeerID) {
                try {
                    connectToPeer(info);
                } catch (IOException e) {
                    Logger.error("Error connecting to peer %d: %s", currentPeerID, e.getMessage());
                }
            }
        });
    }

    // Establishes a connection to a peer
    private void connectToPeer(peerInfo info) throws IOException {
        Socket peerSocket = new Socket(info.getPeerAddress(), info.getPeerPort());
        handshake hs = new handshake(myPeerID);
        DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
        DataInputStream in = new DataInputStream(peerSocket.getInputStream());

        out.write(hs.createHandshake());
        out.flush();

        byte[] response = new byte[32];
        in.readFully(response);
        int receivedPeerID = ByteBuffer.wrap(Arrays.copyOfRange(response, 28, 32)).getInt();
        if (receivedPeerID != Integer.parseInt(info.getPeerID())) {
            throw new IOException("Handshake response from incorrect peer");
        }

        socketToPeerIdMap.put(peerSocket, receivedPeerID);
        FileManager fm = new FileManager(
                configInfo.getFileSize(), configInfo.getPieceSize(),
                configInfo.getConfigFileName(), myPeerInfo.getContainsFile());
        executor.submit(new PeerHandler(peerSocket, fm, interestManager, pieceAvailability));
        ConnectionEventLogger.peerConnected(myPeerID, receivedPeerID);
    }

    // Reads peer information from the configuration file
    public HashMap<String, peerInfo> makePeerInfo(String fileName) throws FileNotFoundException {
        Scanner in = new Scanner(new FileReader(fileName));
        HashMap<String, peerInfo> peersMap = new HashMap<>();

        while (in.hasNextLine()) {
            String[] line = in.nextLine().split(" ");
            peersMap.put(line[0], new peerInfo(line[0], line[1], line[2], line[3]));
        }
        in.close();
        return peersMap;
    }

    // Initializes the pieces based on the peer information
    private void initPieces() {
        int numPieces = getNumPieces();
        allPeerInfo.forEach((key, value) -> {
            BitSet available = new BitSet(numPieces);
            if (value.getContainsFile()) {
                available.set(0, numPieces);
            }
            pieceAvailability.put(Integer.parseInt(key), available);
        });
    }

    // Calculates the number of pieces for the file
    private int getNumPieces() {
        int l = configInfo.getFileSize() / configInfo.getPieceSize();
        if (configInfo.getFileSize() % configInfo.getPieceSize() != 0) {
            l += 1;
        }
        return l;
    }
}
