import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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
import logging.Logger;
import errorhandling.P2PFileSharingException;

public class PeerHandler implements Runnable {
    private Socket peerSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private FileManager fileManager;
    private InterestManager interestManager;
    private HashMap<Integer, BitSet> pieceAvailability;
    private boolean chokedByPeer = false;
    private Map<Socket, Integer> socketToPeerIdMap;
    private int requestedPieceIndex;
    private int remotePeerID;

    public PeerHandler(Socket socket, FileManager fileManager, InterestManager interestManager,
            HashMap<Integer, BitSet> pieceAvailability, Map<Socket, Integer> socketMap) throws IOException {
        this.peerSocket = socket;
        this.in = new DataInputStream(peerSocket.getInputStream());
        this.out = new DataOutputStream(peerSocket.getOutputStream());
        this.fileManager = fileManager;
        this.interestManager = interestManager;
        this.pieceAvailability = pieceAvailability;
        this.socketToPeerIdMap = socketMap;
    }

    private int getPeerIdFromSocket(Socket socket) {
        Logger.info(socketToPeerIdMap.toString());
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
            // Handle error related to peer communication
            // Log this error
            PeerEventLogger.logPeerCommunicationError(getPeerIdFromSocket(peerSocket), e);
        } catch (P2PFileSharingException e) {
            System.err.println("Error in PeerHandler: " + e.getMessage());
            // Handle P2PFileSharingException
            // Log this error
            PeerEventLogger.logPeerCommunicationError(getPeerIdFromSocket(peerSocket), e);
        } finally {
            cleanUpResources();
        }
    }

    private void performHandshake() throws P2PFileSharingException {
        try {
            handshake myHandshake = new handshake(getPeerIdFromSocket(peerSocket));
            Logger.info("handshake peer ID: " + getPeerIdFromSocket(peerSocket));
            byte[] handshakeMessage = myHandshake.createHandshake();
    
            // Log the handshake message being sent
            Logger.info("Sending handshake message " + Arrays.toString(handshakeMessage) + " to peer "
                    + getPeerIdFromSocket(peerSocket));
    
            out.write(handshakeMessage);
            out.flush();
    
            byte[] response = new byte[32];
    
            // Manually read bytes from the input stream
            in.read(response);
            // Log the received handshake response
            Logger.info("Received handshake response: " + Arrays.toString(response) + " from peer "
                    + getPeerIdFromSocket(peerSocket));
    
            int remotePeerID = ByteBuffer.wrap(Arrays.copyOfRange(response, 28, 32)).getInt();
            Logger.info("Extracted remote peer ID: " + remotePeerID);
    
            if (remotePeerID == getPeerIdFromSocket(peerSocket)) {
                throw new IOException("Connected to self.");
            }
    
            socketToPeerIdMap.put(peerSocket, remotePeerID);
    
            // Log connection
            ConnectionEventLogger.peerConnected(getPeerIdFromSocket(peerSocket), remotePeerID);
    
            // Save remotePeerID as a field for future use
            this.remotePeerID = remotePeerID;
        } catch (IOException e) {
            e.printStackTrace();
            throw new P2PFileSharingException("Error during handshake",
                    P2PFileSharingException.ErrorType.HANDSHAKE_ERROR, e);
        }
    }
    

    private void handleMessage(byte[] message) throws P2PFileSharingException {
        try {
            char messageType = getMessageTypeFromMessage(message);
            switch (messageType) {
                case '0':
                    handleChoke();
                    break;
                case '1':
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
        } catch (Exception e) {
            throw new P2PFileSharingException("Error handling message: " + e.getMessage(),
                    P2PFileSharingException.ErrorType.MESSAGE_ERROR, e);
        }
    }

    private void handleChoke() {
        chokedByPeer = true;
        PeerEventLogger.peerChoked(getPeerIdFromSocket(peerSocket), remotePeerID);
    }

    private void handleUnchoke() throws P2PFileSharingException {
        chokedByPeer = false;
        PeerEventLogger.peerUnchoked(getPeerIdFromSocket(peerSocket), remotePeerID);
        requestNeededPieces();
    }

    private void requestNeededPieces() throws P2PFileSharingException {
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

    private void requestPiece(int pieceIndex) throws P2PFileSharingException {
        try {
            byte[] requestMessage = createRequestMessage(pieceIndex);
            out.write(requestMessage);
            ConnectionEventLogger.dataRequestSent(getPeerIdFromSocket(peerSocket), remotePeerID, pieceIndex);
        } catch (IOException e) {
            throw new P2PFileSharingException("Error requesting piece: " + e.getMessage(),
                    P2PFileSharingException.ErrorType.MESSAGE_ERROR, e);
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

    private void handleRequest(byte[] message) throws P2PFileSharingException {
        // Extract the requested piece index from the message
        requestedPieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
        // Perform the rest of the request logic, potentially sending a piece back
        sendRequestedPiece();
    }

    private void sendRequestedPiece() throws P2PFileSharingException {
        if (!chokedByPeer) {
            try {
                fileManager.sendPiece(requestedPieceIndex, out);
            } catch (P2PFileSharingException e) {

                System.err.println(
                        "IOException occurred while sending piece " + requestedPieceIndex + ": " + e.getMessage());
                // Handle exception by logging or sending a different message
            }
        } else {
            System.out.println("Cannot send piece " + requestedPieceIndex + " as we are choked by the peer.");
        }
    }

    private void handlePiece(byte[] message) throws P2PFileSharingException {
        try {
            int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
            byte[] pieceData = Arrays.copyOfRange(message, 5, message.length);
            fileManager.storePiece(pieceIndex, pieceData);
            PeerEventLogger.pieceDownloaded(getPeerIdFromSocket(peerSocket), remotePeerID, pieceIndex,
                    pieceData.length);
        } catch (Exception e) {
            throw new P2PFileSharingException("Error handling piece message: " + e.getMessage(),
                    P2PFileSharingException.ErrorType.FILE_ERROR, e);
        }
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
            // Optionally, you might want to log this error as well
        }
    }

}
