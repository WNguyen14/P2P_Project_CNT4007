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
import java.io.EOFException;
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
        Logger.info("Initializing peer process with peerID: " + myPeerID);
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
                        executor.submit(new PeerHandler(clientSocket, fm, interestManager, pieceAvailability, socketToPeerIdMap));
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
        DataInputStream in = null;
        try {
            in = new DataInputStream(socket.getInputStream());
            byte[] handshake = new byte[32];
    
            // Read the handshake message from the input stream
            int bytesRead = in.read(handshake);
            Logger.info("Bytes read for handshake: " + bytesRead);
            Logger.info("Received handshake bytes: " + Arrays.toString(handshake));
    
            // Extracting the peer ID from the last 4 bytes
            ByteBuffer buffer = ByteBuffer.wrap(handshake, 28, 4);
            int extractedPeerId = buffer.getInt();
            Logger.info("Extracted peer ID: " + extractedPeerId);
    
            return extractedPeerId;
        } catch (IOException e) {
            Logger.error("Error in handshake: " + e.getMessage());
            throw e;
        } finally {
            // Optional: Close the input stream if you're done with it
            // if (in != null) in.close();
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
                } catch (P2PFileSharingException e) {
                    Logger.error("Error connecting to peer %d: %s", currentPeerID, e.getMessage());
                }
            }
        });
    }

    private void connectToPeer(peerInfo info) throws P2PFileSharingException, IOException {
        Logger.info("Connecting to peer with ID: " + info.getPeerID() + ", myPeerID: " + myPeerID);

        Socket peerSocket = null;
        try {
            peerSocket = new Socket(info.getPeerAddress(), info.getPeerPort());
            handshake hs = new handshake(myPeerID);
            DataOutputStream out = new DataOutputStream(peerSocket.getOutputStream());
            byte[] handshakeMessage = hs.createHandshake();
    
            Logger.info("Sending handshake to peer " + info.getPeerID());
            out.write(handshakeMessage);
            out.flush();
    
            DataInputStream in = new DataInputStream(peerSocket.getInputStream());
            byte[] response = new byte[32];
            in.readFully(response);
    
            int receivedPeerID = ByteBuffer.wrap(Arrays.copyOfRange(response, 28, 32)).getInt();
    
            if (receivedPeerID != Integer.parseInt(info.getPeerID())) {
                Logger.error("Incorrect peer ID received in handshake: " + receivedPeerID + " != " + info.getPeerID());
                throw new P2PFileSharingException("Incorrect peer ID received in handshake", P2PFileSharingException.ErrorType.HANDSHAKE_ERROR);
            }
    
            socketToPeerIdMap.put(peerSocket, receivedPeerID);
    
            FileManager fm = new FileManager(configInfo.getFileSize(), configInfo.getPieceSize(),
                    configInfo.getConfigFileName(), myPeerInfo.getContainsFile(), myPeerID);
            executor.submit(new PeerHandler(peerSocket, fm, interestManager, pieceAvailability, socketToPeerIdMap));
            Logger.info("Connected to peer " + receivedPeerID);
    
        } catch (IOException e) {
            Logger.error("IO error in peer connection to " + info.getPeerID() + ": " + e.getMessage());
            if (peerSocket != null) {
                try {
                    peerSocket.close();
                } catch (IOException ex) {
                    Logger.error("Error closing socket: " + ex.getMessage());
                }
            }
            throw e;
        } catch (NumberFormatException e) {
            Logger.error("Error parsing peer ID: " + e.getMessage());
            throw new P2PFileSharingException("Peer ID format error", P2PFileSharingException.ErrorType.MESSAGE_ERROR, e);
        } catch (P2PFileSharingException e) {
            Logger.error("Error in handshake file thing: " + e.getMessage());
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
