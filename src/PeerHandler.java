import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PeerHandler implements Runnable {
    private Socket peerSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private FileManager fileManager;
    private InterestManager interestManager;
    private boolean chokedByPeer = false;
    private static final Map<Socket, Integer> socketToPeerIdMap = new ConcurrentHashMap<>();

    // Variable to store the piece index requested by this peer
    private int requestedPieceIndex;

    public PeerHandler(Socket socket, FileManager fileManager, InterestManager interestManager) throws IOException {
        this.peerSocket = socket;
        this.in = new DataInputStream(peerSocket.getInputStream());
        this.out = new DataOutputStream(peerSocket.getOutputStream());
        this.fileManager = fileManager;
        this.interestManager = interestManager;
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

    private void performHandshake() throws IOException {
        handshake myHandshake = new handshake(getPeerIdFromSocket(peerSocket));
        byte[] handshakeMessage = myHandshake.createHandshake();

        out.write(handshakeMessage);
        out.flush();

        byte[] response = new byte[32];
        in.readFully(response);

        int remotePeerID = handshake.readHandshake(response);
        if (remotePeerID == getPeerIdFromSocket(peerSocket)) {
            throw new IOException("Connected to self.");
        }
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

    private void handleChoke() {
        // When we're choked by a peer, we should stop sending 'request' messages
        // This typically means updating a flag that tracks whether we're choked by this
        // peer
        // This could also trigger some kind of event or state change in the peer's
        // logic
        System.out.println("Choked by peer. Stopping requests.");
        // Set a flag or notify the system that this peer has choked us
        // e.g., this.chokedByPeer = true;
    }

    private void handleUnchoke() {
        // When we're unchoked by a peer, we can start requesting pieces again
        System.out.println("Unchoked by peer. Can request pieces now.");
        // Set a flag or notify the system that this peer has unchoked us
        // e.g., this.chokedByPeer = false;
        // And then start requesting pieces that we need
        // e.g., requestNeededPieces();
    }

    private void handleInterested() {
        int peerId = getPeerIdFromSocket(peerSocket);
        // Logic to determine which pieces are interesting to this peer
        // For example, check against the bitfield to see if we have pieces that the
        // other peer is interested in
        // Add those piece indices to the interest manager
        for (int pieceIndex : getInterestingPieces(peerId)) {
            interestManager.addInterestedPeer(peerId, pieceIndex);
        }
        System.out.println("Peer " + peerId + " is interested.");
    }

    private void handleNotInterested() {
        int peerId = getPeerIdFromSocket(peerSocket);
        // Remove all entries for this peer from the interest manager, as they are not
        // interested in any pieces
        interestManager.removeAllInterestedPieces(peerId);
        System.out.println("Peer " + peerId + " is not interested.");
    }

    // Additional helper method to determine interesting pieces
    private Set<Integer> getInterestingPieces(int peerId) {
        // Implement logic to determine interesting pieces based on bitfields
        Set<Integer> interestingPieces = new HashSet<>();
        // Example logic:
        // if (/* condition to check if piece is interesting */) {
        // interestingPieces.add(/* piece index */);
        // }
        return interestingPieces;
    }

    private void handleHave(byte[] message) {
        // Implement have logic, updating the bitfield for the peer that sent this
        // message
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
        fileManager.updateHave(pieceIndex);
    }

    private void handleBitfield(byte[] message) {
        // Implement bitfield logic, updating the bitfield for the peer that sent this
        // message
        BitSet bitfield = BitSet.valueOf(Arrays.copyOfRange(message, 1, message.length));
        fileManager.updateBitfield(bitfield);
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
