import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import logging.ConnectionEventLogger;
import errorhandling.P2PFileSharingException;

public class handshake {

    private static final String HEADER = "P2PFILESHARINGPROJ";
    private static final int ZERO_BITS_LENGTH = 10;
    private static final int PEER_ID_LENGTH = 4;
    private int peerID;

    public handshake(int peerID) {
        this.peerID = peerID;
    }

    public byte[] createHandshake() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(HEADER.getBytes(StandardCharsets.UTF_8));
            stream.write(new byte[ZERO_BITS_LENGTH]);
            stream.write(ByteBuffer.allocate(PEER_ID_LENGTH).putInt(this.peerID).array());
            // Log handshake start event
            ConnectionEventLogger.peerHandshakeStarted(this.peerID, -1); // -1 indicating unknown peer ID
        } catch (IOException e) {

            // SHOULDNt be here
            throw new RuntimeException("Error constructing the handshake message", e);
        }
        return stream.toByteArray();
    }

    public static int readHandshake(byte[] handshakeMessage) throws P2PFileSharingException {
        if (handshakeMessage.length != HEADER.length() + ZERO_BITS_LENGTH + PEER_ID_LENGTH) {
            throw new P2PFileSharingException("Handshake message is not the correct size.", P2PFileSharingException.ErrorType.MESSAGE_ERROR);
        }

        String receivedHeader = new String(handshakeMessage, 0, HEADER.length(), StandardCharsets.UTF_8);
        if (!HEADER.equals(receivedHeader)) {
            throw new P2PFileSharingException("Handshake failed: Incorrect header.", P2PFileSharingException.ErrorType.HANDSHAKE_ERROR);
        }

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
