import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

public class PeerHandler implements Runnable {
    private Socket peerSocket;
    private DataInputStream in;
    private DataOutputStream out;
    private FileManager fileManager; // Assuming FileManager is properly implemented

    public PeerHandler(Socket socket, FileManager fileManager) throws IOException {
        this.peerSocket = socket;
        this.in = new DataInputStream(peerSocket.getInputStream());
        this.out = new DataOutputStream(peerSocket.getOutputStream());
        this.fileManager = fileManager; // Use the FileManager passed from peerProcess
    }

    @Override
    public void run() {
        try {
            // Initial handshake and bitfield exchange.
            performHandshake();

            // Normal operation (receiving messages and responding accordingly)
            while (true) {
                if (peerSocket.isClosed()) {
                    break;
                }

                // Read message
                int length = in.readInt();
                if (length > 0) {
                    byte[] message = new byte[length];
                    in.readFully(message);  // Read the message

                    handleMessage(message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error in PeerHandler: " + e.getMessage());
            // Handle exception
        } finally {
            // Clean up resources
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (peerSocket != null) peerSocket.close();
            } catch (IOException e) {
                // Handle closing exception
            }
        }
    }

    private void performHandshake() throws IOException {
        // TODO: Implement handshake logic here
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
        // Implement choke logic
    }

    private void handleUnchoke() {
        // Implement unchoke logic
    }

    private void handleInterested() {
        // Implement interested logic
    }

    private void handleNotInterested() {
        // Implement not interested logic
    }

    private void handleHave(byte[] message) {
        // Implement have logic, updating the bitfield for the peer that sent this message
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
        fileManager.updateHave(pieceIndex);
    }

    private void handleBitfield(byte[] message) {
        // Implement bitfield logic, updating the bitfield for the peer that sent this message
        BitSet bitfield = BitSet.valueOf(Arrays.copyOfRange(message, 1, message.length));
        fileManager.updateBitfield(bitfield);
    }

    private void handleRequest(byte[] message) {
        // Implement request logic, sending a piece back if not choked
        int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)).getInt();
        try {
            fileManager.sendPiece(pieceIndex, out);
        } catch (IOException e) {
            System.err.println("IOException occurred while sending a piece: " + e.getMessage());
            // Handle exception by logging or sending a different message
         }
    }
    

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
}
