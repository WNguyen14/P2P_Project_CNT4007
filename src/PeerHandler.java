import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

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
        // TODO: Implement message handling logic here
        // This is where you would parse the message type and take action accordingly
    }
}
