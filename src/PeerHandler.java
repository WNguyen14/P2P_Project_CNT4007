import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import logging.ConnectionEventLogger;
import logging.PeerEventLogger;
import java.io.File;
import java.io.RandomAccessFile;

public class PeerHandler implements Runnable {
    private Socket peerSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private FileManager fileManager;
    private InterestManager interestManager;
    private HashMap<Integer, BitSet> pieceAvailability;
    private boolean chokedByPeer = false;

    private static final Map<Socket, Integer> socketToPeerIdMap = new ConcurrentHashMap<>();

    // Variable to store the piece index requested by this peer
    private int requestedPieceIndex;

    public PeerHandler(Socket socket, FileManager fileManager, InterestManager interestManager,
            HashMap<Integer, BitSet> pieceAvailability) throws IOException {
        this.peerSocket = socket;
        this.in = new DataInputStream(peerSocket.getInputStream());
        this.out = new DataOutputStream(peerSocket.getOutputStream());
        this.fileManager = fileManager;
        this.interestManager = interestManager;
        this.pieceAvailability = pieceAvailability;

        int peerId = getPeerIdFromSocket(socket);
        File peerDir = new File("~/project/peer_" + peerId);
        if (!peerDir.exists()) {
            peerDir.mkdirs(); // Create directory if it doesn't exist
        }
        // Additional initialization for file handling based on the directory
    }

    private int getPeerIdFromSocket(Socket socket) {
        return socketToPeerIdMap.getOrDefault(socket, -1);
    }

    @Override
    public void run() {
        try {
            performHandshake();

            while (true) {
                if (peerSocket.isClosed()) {
                    break;
                }

                int length = in.readInt();
                if (length > 0) {
                    byte[] message = new byte[length];
                    in.readFully(message);
                    handleMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error in PeerHandler: " + e.getMessage());
        } finally {
            cleanUpResources();
        }
    }

    // In PeerHandler.java
    private void performHandshake() throws IOException {
        // Create a handshake object with the current peer's ID
        handshake myHandshake = new handshake(getPeerIdFromSocket(peerSocket));

        // Send the handshake message
        out.write(myHandshake.createHandshake());
        out.flush();

        // Read the handshake response
        byte[] response = new byte[32];
        in.readFully(response);

        // Extract the peer ID from the response
        int remotePeerID = ByteBuffer.wrap(Arrays.copyOfRange(response, 28, 32)).getInt();

        // Check if the handshake is self-connection
        if (remotePeerID == getPeerIdFromSocket(peerSocket)) {
            throw new IOException("Connected to self.");
        }

        // Store the mapping of the socket to peer ID
        socketToPeerIdMap.put(peerSocket, remotePeerID);

        System.out.println("Handshake successful with peer: " + remotePeerID);
    }

    private void handleMessage(byte[] message) throws IOException {
        // Parse the message type
        char messageType = getMessageTypeFromMessage(message);
        switch (messageType) {
            case '0': // choke
                handleChoke();
                break;
            case '1': // unchoke
                handleUnchoke();
                break;
            case '2': // interested
                handleInterested();
                break;
            case '3': // not interested
                handleNotInterested();
                break;
            case '4': // have
                handleHave(message);
                break;
            case '5': // bitfield
                handleBitfield(message);
                break;
            case '6': // request
                handleRequest(message);
                break;
            case '7': // piece
                handlePiece(message);
                break;
            default:
                System.out.println("Unknown message type received: " + messageType);
        }
    }

    // In PeerHandler.java
    private void handleChoke() {
        chokedByPeer = true;
        // Additional logic when choked
        System.out.println("Choked by peer " + getPeerIdFromSocket(peerSocket));
    }

    private void handleUnchoke() {
        chokedByPeer = false;
        // Additional logic when unchoked (like requesting pieces)
        System.out.println("Unchoked by peer " + getPeerIdFromSocket(peerSocket));
        requestNeededPieces();
    }

    private void requestNeededPieces() {
        int peerId = getPeerIdFromSocket(peerSocket);
        BitSet myBitfield = fileManager.getBitfield();
        BitSet peerBitfield = pieceAvailability.get(peerId);

        if (peerBitfield != null) {
            for (int i = 0; i < peerBitfield.length(); i++) {
                if (peerBitfield.get(i) && !myBitfield.get(i)) {
                    requestPiece(i);
                    break; // Break after requesting one piece to avoid flooding
                }
            }
        }
    }

    private void requestPiece(int pieceIndex) {
        try {
            // Creating a request message for the specified piece index
            byte[] requestMessage = createRequestMessage(pieceIndex);
            out.write(requestMessage);
        } catch (IOException e) {
            System.err.println("Error sending request message for piece " + pieceIndex + ": " + e.getMessage());
        }
    }

    private byte[] createRequestMessage(int pieceIndex) {
        // Assuming the message format is: [message length (4 bytes)] + [message type (1
        // byte)] + [piece index (4 bytes)]
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.putInt(5); // Length of the message (1 byte for type + 4 bytes for piece index)
        buffer.put((byte) '6'); // Message type '6' for request
        buffer.putInt(pieceIndex); // The requested piece index

        return buffer.array();
    }

    private void handleInterested() {
        int peerId = getPeerIdFromSocket(peerSocket);

        // Assuming getInterestingPieces returns the indices of the pieces that
        // this peer has and the other peer doesn't. This should be based on comparing
        // the two bitfields: the current peer's and the other peer's.
        Set<Integer> interestingPieces = getInterestingPieces(peerId);

        // If there are interesting pieces, mark the peer as interested in those pieces.
        if (!interestingPieces.isEmpty()) {
            for (int pieceIndex : interestingPieces) {
                interestManager.addInterestedPeer(peerId, pieceIndex);
            }
            System.out.println("Peer " + peerId + " is interested in pieces: " + interestingPieces);
        } else {
            // If there are no interesting pieces, handle as not interested.
            handleNotInterested(); // can call this directly if no interesting pieces.
        }
    }

    private void handleNotInterested() {
        int peerId = getPeerIdFromSocket(peerSocket);

        // Remove all interest entries for this peer since they are not interested in
        // any pieces.
        interestManager.removeAllInterestedPieces(peerId);
        System.out.println("Peer " + peerId + " is not interested in any more pieces.");
    }

    // This method will compare the bitfields of this peer and another peer
    // to find out which pieces the other peer has that this peer doesn't.
    private Set<Integer> getInterestingPieces(int peerId) {

        // Todo fix this string bs cause might get an error here
        BitSet myBitfield = pieceAvailability.get(getPeerIdFromSocket(peerSocket)); // own bitfield
        BitSet otherPeerBitfield = pieceAvailability.get(peerId); // The other peer's bitfield

        Set<Integer> interestingPieces = new HashSet<>();
        if (otherPeerBitfield != null) {
            // Here we go through each bit in the other peer's bitfield.
            for (int i = 0; i < myBitfield.length(); i++) {
                // If the other peer has a piece that we don't, it's interesting to us.
                if (otherPeerBitfield.get(i) && !myBitfield.get(i)) {
                    interestingPieces.add(i);
                }
            }
        }
        return interestingPieces;
    }

    private void handleHave(byte[] message) {
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
        fileManager.updateHave(pieceIndex);

        int remotePeerID = getPeerIdFromSocket(peerSocket);
        BitSet peerBitfield = pieceAvailability.get(remotePeerID);
        peerBitfield.set(pieceIndex);

        Set<Integer> interestingPieces = getInterestingPieces(remotePeerID);
        if (!interestingPieces.isEmpty()) {
            sendInterestedMessage();
        } else {
            sendNotInterestedMessage();
        }
    }

    private void handleBitfield(byte[] messageBytes) {
        BitSet receivedBitfield = message.parseBitfieldMessage(messageBytes);

        // Update the piece availability for the remote peer
        int remotePeerID = getPeerIdFromSocket(peerSocket);
        pieceAvailability.put(remotePeerID, receivedBitfield);

        // After updating the bitfield, check if interested
        Set<Integer> interestingPieces = getInterestingPieces(remotePeerID);
        if (!interestingPieces.isEmpty()) {
            sendInterestedMessage();
        } else {
            sendNotInterestedMessage();
        }
    }

    private void sendInterestedMessage() {
        try {
            out.write(new message('2').getMessage());
        } catch (IOException e) {
            System.err.println("Error sending interested message: " + e.getMessage());
        }
    }

    private void sendNotInterestedMessage() {
        try {
            out.write(new message('3').getMessage());
        } catch (IOException e) {
            System.err.println("Error sending not interested message: " + e.getMessage());
        }
    }

    private void handleRequest(byte[] message) {
        // Extract the requested piece index from the message
        requestedPieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
        // Perform the rest of the request logic, potentially sending a piece back
        sendRequestedPiece();
    }

    private void sendRequestedPiece() {
        if (!chokedByPeer) {
            try {
                fileManager.sendPiece(requestedPieceIndex, out);
            } catch (IOException e) {
                System.err.println(
                        "IOException occurred while sending piece " + requestedPieceIndex + ": " + e.getMessage());
                // Handle exception by logging or sending a different message
            }
        } else {
            System.out.println("Cannot send piece " + requestedPieceIndex + " as we are choked by the peer.");
        }
    }

    // I need validation on this
    private void handlePiece(byte[] message) {
        // Implement piece logic, storing the received piece
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
        byte[] pieceData = Arrays.copyOfRange(message, 5, message.length);
        fileManager.storePiece(pieceIndex, pieceData);
    }

    // Helper methods to extract message type and other information from the message
    public char getMessageTypeFromMessage(byte[] message) {
        return (char) message[0]; // Assuming message type is the first byte
    }

    private void cleanUpResources() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (peerSocket != null)
                peerSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }
}
