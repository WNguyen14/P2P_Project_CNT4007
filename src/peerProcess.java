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
import logging.Logger;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import errorhandling.P2PFileSharingException;

/**
 * The main class for starting and managing peer processes in a P2P network.
 * This class initializes peer connections, handles incoming connections,
 * and manages the server socket.
 */
public class peerProcess {

    private int myPeerID;
    private Config configInfo;
    private HashMap<String, peerInfo> allPeerInfo;
    private HashMap<Integer, BitSet> pieceAvailability;
    private peerInfo myPeerInfo;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    private static final Map<Socket, Integer> socketToPeerIdMap = new ConcurrentHashMap<>();

    private InterestManager interestManager = new InterestManager();

    public static void main(String[] args) {
        try {
            if (args.length != 1) {
                Logger.error("Usage: java peerProcess <peerID>");
                return;
            }
            int peerID = Integer.parseInt(args[0]);
            new peerProcess(peerID).start();
        } catch (Exception e) {
            Logger.error("Failed to start peer process: %s", e.getMessage());
            System.exit(1);
        }
    }

    public peerProcess(int myPeerID) throws FileNotFoundException, P2PFileSharingException {
        this.myPeerID = myPeerID;
        try {
            this.configInfo = new Config("Common.cfg");
            this.allPeerInfo = makePeerInfo("PeerInfo.cfg");
            this.myPeerInfo = allPeerInfo.get(Integer.toString(myPeerID));
            this.pieceAvailability = new HashMap<>();
            this.executor = Executors.newCachedThreadPool();
            Logger.info("Peer process for peerID %d created", myPeerID);
        } catch (FileNotFoundException e) {
            throw new P2PFileSharingException("Config file not found", P2PFileSharingException.ErrorType.FILE_ERROR, e);
        }
    }

    private void start() throws IOException {
        Logger.info("Peer %d starting...", myPeerID);
        initPieces();

        startServer();
        connectToPreviousPeers();

        Logger.info("Peer %d successfully initialized", myPeerID);
    }

    private void startServer() throws IOException {
        int myPort = this.myPeerInfo.getPeerPort();
        this.serverSocket = new ServerSocket(myPort);
        executor.submit(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int peerId = determinePeerId(clientSocket);
                    socketToPeerIdMap.put(clientSocket, peerId);
                    try {
                        FileManager fm = new FileManager(configInfo.getFileSize(), configInfo.getPieceSize(),
                                configInfo.getConfigFileName(), myPeerInfo.getContainsFile(), myPeerID);
                        executor.submit(new PeerHandler(clientSocket, fm, interestManager, pieceAvailability));
                    } catch (P2PFileSharingException e) {
                        Logger.error("Failed to initialize FileManager: %s", e.getMessage());
                        // REEEEEEEEEEEEEEEEEEEE
                        // mweow
                    }
                } catch (IOException e) {
                    Logger.error("Error accepting connection: %s", e.getMessage());
                }
            }
        });
        Logger.info("Server started, listening on port %d", myPort);
    }

    private int determinePeerId(Socket socket) throws IOException {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            byte[] handshake = new byte[32];
            in.readFully(handshake);
            return ByteBuffer.wrap(Arrays.copyOfRange(handshake, 28, 32)).getInt();
        } catch (IOException e) {
            Logger.error("Error in handshake: %s", e.getMessage());
            throw e;
        }
    }

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

    private void connectToPeer(peerInfo info) throws IOException {
        try {
            Socket peerSocket = new Socket(info.getPeerAddress(), info.getPeerPort());
            handshake hs = new handshake(myPeerID);
            DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
            out.write(hs.createHandshake());
            out.flush();
            DataInputStream in = new DataInputStream(peerSocket.getInputStream());
            byte[] response = new byte[32];
            in.readFully(response);
            int receivedPeerID = ByteBuffer.wrap(Arrays.copyOfRange(response, 28, 32)).getInt();
            if (receivedPeerID != Integer.parseInt(info.getPeerID())) {
                throw new IOException("Incorrect peer ID received in handshake");
            }
            socketToPeerIdMap.put(peerSocket, receivedPeerID);

            try {
                FileManager fm = new FileManager(configInfo.getFileSize(), configInfo.getPieceSize(),
                        configInfo.getConfigFileName(), myPeerInfo.getContainsFile(), myPeerID);
                executor.submit(new PeerHandler(peerSocket, fm, interestManager, pieceAvailability));
                Logger.info("Connected to peer %d", receivedPeerID);
            } catch (P2PFileSharingException e) {
                Logger.error("Failed to initialize FileManager: %s", e.getMessage());
            }

        } catch (IOException e) {
            Logger.error("Error in peer connection: %s", e.getMessage());
            throw e;
        }
    }

    public HashMap<String, peerInfo> makePeerInfo(String fileName) throws FileNotFoundException {
        try {
            Scanner in = new Scanner(new FileReader(fileName));
            HashMap<String, peerInfo> peersMap = new HashMap<>();
            while (in.hasNextLine()) {
                String[] line = in.nextLine().split(" ");
                peerInfo newPeer = new peerInfo(line[0], line[1], line[2], line[3]);
                peersMap.put(line[0], newPeer);
            }
            in.close();
            return peersMap;
        } catch (FileNotFoundException e) {
            Logger.error("Error reading PeerInfo.cfg: %s", e.getMessage());
            throw e;
        }
    }

    private void initPieces() {
        int numPieces = getNumPieces();
        allPeerInfo.forEach((id, info) -> {
            BitSet available = new BitSet(numPieces);
            if (info.getContainsFile()) {
                available.set(0, numPieces);
            }
            pieceAvailability.put(Integer.parseInt(id), available);
        });
        Logger.info("Pieces initialized for peer %d", myPeerID);
    }

    public BitSet getPeerBitfield(String peerId) {
        return pieceAvailability.get(peerId);
    }

    private int getNumPieces() {
        return (configInfo.getFileSize() + configInfo.getPieceSize() - 1) / configInfo.getPieceSize();
    }
}
