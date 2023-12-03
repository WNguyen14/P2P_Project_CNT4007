import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class handshake {

    private static final String HEADER = "P2PFILESHARINGPROJ";
    private static final int ZERO_BITS_LENGTH = 10;
    private static final int PEER_ID_LENGTH = 4;
    private String peerID;

    public handshake(String peerID) {
        this.peerID = peerID;
    }

    public byte[] createHandshake() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            // header is first 18 bytes
            stream.write(HEADER.getBytes(StandardCharsets.UTF_8));
            // then is 10 zero bytes
            stream.write(new byte[ZERO_BITS_LENGTH]);
            // tAhen last 4 bytes is peer id
            stream.write(this.peerID.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // This should never happen with ByteArrayOutputStream
            throw new RuntimeException("Error constructing the handshake message", e);
        }
        return stream.toByteArray();
    }

    public static String readHandshake(byte[] handshakeMessage) throws IOException {
        if (handshakeMessage.length != HEADER.length() + ZERO_BITS_LENGTH + PEER_ID_LENGTH) {
            throw new IOException("Handshake message is not the correct size.");
        }

        // Check the header
        String receivedHeader = new String(handshakeMessage, 0, HEADER.length(), StandardCharsets.UTF_8);
        if (!HEADER.equals(receivedHeader)) {
            throw new IOException("Handshake failed: Incorrect header.");
        }

        // Extract the peer ID
        return new String(handshakeMessage, HEADER.length() + ZERO_BITS_LENGTH, PEER_ID_LENGTH, StandardCharsets.UTF_8);
    }

    public String getPeerID() {
        return this.peerID;
    }

    public void setPeerID(String peerID) {
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
