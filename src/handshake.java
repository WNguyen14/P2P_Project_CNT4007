/*
handshake works both ways, getHandshake to send a handshake to a peer, readHandshake to recieve the handshake and look at it
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class handshake {

	private String header;
	private String peerID;

	public handshake(String peerID){
		this.header = "P2PFILESHARINGPROJ";
		this.peerID = peerID;
	}

	public byte[] getHandshake() {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		try {
			// header is first 18 bytes
			s.write(this.header.getBytes(StandardCharsets.UTF_8));
			// then is 10 zero bytes
			s.write(new byte[10]);
			// then last 4 bits is peer id
			s.write(this.peerID.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return s.toByteArray();
	}

	public void readHandshake(byte[] msg) {
		String s = new String(msg, StandardCharsets.UTF_8);
		// last 4 bits is peer id
		this.peerID = s.substring(28,32);
	}

}
