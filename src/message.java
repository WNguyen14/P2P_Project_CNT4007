/*
class to do work on messages (not handshake messages)
 */
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class message {

	private int messageLength;
	private char messageType;
	private byte[] messagePayload;

	public message(char type) {
		this.messageType = type;
		this.messagePayload = new byte[0];
	}

	public byte[] getMessage() {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		try {
			byte[] bytes = ByteBuffer.allocate(4).putInt(this.messageLength).array();
			s.writeBytes(bytes);
			s.write(this.messageType);
			s.writeBytes(this.messagePayload);
		}
		catch (Exception e){
			throw new RuntimeException(e);
		}
		return s.toByteArray();
	}

	public int getMessageLengthFromMessage(byte[] message) {
		byte[] bytes = Arrays.copyOfRange(message, 0, 3);
		ByteBuffer wrapped = ByteBuffer.wrap(bytes);
		return wrapped.getShort();
	}

	public char getMessageTypeFromMessage(byte[] message) {
		byte[] bytes = Arrays.copyOfRange(message, 4, 5);
		ByteBuffer wrapped = ByteBuffer.wrap(bytes);
		return wrapped.getChar();
	}

	public byte[] getMessagePayloadFromMessage(byte[] message) {
		return Arrays.copyOfRange(message, 5, message.length);
	}

	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}
}
