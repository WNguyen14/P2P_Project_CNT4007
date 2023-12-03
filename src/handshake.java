import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class handshake {

    private static final String HEADER = "P2PFILESHARINGPROJ";
    private static final int ZERO_BITS_LENGTH = 10;
    private static final int PEER_ID_LENGTH = 4;
    private int peerID; // Changed to int

    public handshake(int peerID) {
        this.peerID = peerID;
    }

    public byte[] createHandshake() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            // header is first 18 bytes
            stream.write(HEADER.getBytes(StandardCharsets.UTF_8));
            // then is 10 zero bytes
            stream.write(new byte[ZERO_BITS_LENGTH]);
            // then last 4 bytes is peer id
            stream.write(ByteBuffer.allocate(PEER_ID_LENGTH).putInt(this.peerID).array());
        } catch (IOException e) {
            // This should never happen with ByteArrayOutputStream
            throw new RuntimeException("Error constructing the handshake message", e);
        }
        return stream.toByteArray();
    }

    public static int readHandshake(byte[] handshakeMessage) throws IOException {
        if (handshakeMessage.length != HEADER.length() + ZERO_BITS_LENGTH + PEER_ID_LENGTH) {
            throw new IOException("Handshake message is not the correct size.");
        }

        // Check the header
        String receivedHeader = new String(handshakeMessage, 0, HEADER.length(), StandardCharsets.UTF_8);
        if (!HEADER.equals(receivedHeader)) {
            throw new IOException("Handshake failed: Incorrect header.");
        }

        // Extract the peer ID
        ByteBuffer buffer = ByteBuffer.wrap(handshakeMessage, HEADER.length() + ZERO_BITS_LENGTH, PEER_ID_LENGTH);
        return buffer.getInt();
    }

    public int getPeerID() {
        return this.peerID;
    }

    public void setPeerID(int peerID) {
        this.peerID = peerID;
    }

    public static String getHeader() {
        return HEADER;
    }

    public static int getZeroBitsLength() {
        return ZERO_BITS_LENGTH;
    }

    public static int getPeerIdLength() {
        return PEER_ID_LENGTH;
    }
}
