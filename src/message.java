import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import logging.PeerEventLogger;
import errorhandling.P2PFileSharingException;

/**
 * Class for handling message operations in a P2P file sharing application.
 * This class provides functionalities to create, parse, and manage different types of messages
 * exchanged between peers in the network.
 */
public class message {

    private int messageLength;
    private char messageType;
    private byte[] messagePayload;

    /**
     * Constructor for creating a message with a specific type.
     * @param type The type of the message.
     */
    public message(char type) {
        this.messageType = type;
        this.messagePayload = new byte[0];
    }

    /**
     * Constructs the message into a byte array format to be sent over the network.
     * @return The byte array representation of the message.
     */
    public byte[] getMessage() {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        try {
            byte[] bytes = ByteBuffer.allocate(4).putInt(this.messageLength).array();
            s.writeBytes(bytes);
            s.write(this.messageType);
            s.writeBytes(this.messagePayload);
        } catch (Exception e) {
            // Logging the error when creating the message
            PeerEventLogger.logPeerCommunicationError(messageType, e);
            throw new RuntimeException("Error creating message", e);
        }
        return s.toByteArray();
    }

    /**
     * Creates a bitfield message to be sent to other peers.
     * @param bitfield The BitSet representing the pieces the peer has.
     * @return The byte array of the bitfield message.
     */
    public static byte[] createBitfieldMessage(BitSet bitfield) {
        byte[] bitfieldArray = bitfield.toByteArray();
        byte[] header = ByteBuffer.allocate(5).putInt(1 + bitfieldArray.length).put((byte) '5').array();
        ByteArrayOutputStream messageStream = new ByteArrayOutputStream();
        try {
            messageStream.write(header);
            messageStream.write(bitfieldArray);
        } catch (IOException e) {
            // Logging the error while creating bitfield message
            PeerEventLogger.logPeerCommunicationError('5', e);
            throw new RuntimeException("Error creating bitfield message", e);
        }
        return messageStream.toByteArray();
    }

    /**
     * Parses a received bitfield message.
     * @param message The received bitfield message.
     * @return The BitSet parsed from the message.
     */
    public static BitSet parseBitfieldMessage(byte[] message) {
        return BitSet.valueOf(Arrays.copyOfRange(message, 5, message.length));
    }

    // Additional methods to work with message attributes

    public int getMessageLengthFromMessage(byte[] message) {
        byte[] bytes = Arrays.copyOfRange(message, 0, 4);
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        return wrapped.getInt();
    }

    public char getMessageTypeFromMessage(byte[] message) {
        return (char) message[4];
    }

    public byte[] getMessagePayloadFromMessage(byte[] message) {
        return Arrays.copyOfRange(message, 5, message.length);
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
    }
}