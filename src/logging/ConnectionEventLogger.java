package logging;

import java.io.IOException;

/**
 * ConnectionEventLogger class for logging various connection-related events.
 * This class provides specialized methods to log events related to connections between peers in the P2P network.
 */
public class ConnectionEventLogger {

    /**
     * Logs a message when a peer establishes a connection with another peer.
     *
     * @param peerId1 the ID of the initiating peer
     * @param peerId2 the ID of the receiving peer
     */
    public static void peerConnected(int peerId1, int peerId2) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] makes a connection to Peer [%d]", peerId1, peerId2);
                LogWriter.writeToFile("log_peer_"+peerId1+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // NOTE: BELOW LOGS AREN'T NEEDED FOR PROJECT SPEC, BUT HELPFUL FOR DEBUGGING

    /**
     * Logs a message when a peer disconnects from another peer.
     *
     * @param peerId1 the ID of the disconnecting peer
     * @param peerId2 the ID of the disconnected peer
     */
    public static void peerDisconnected(int peerId1, int peerId2) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] disconnects from Peer [%d]", peerId1, peerId2);
                LogWriter.writeToFile("log_peer_"+peerId1+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer starts a handshake process.
     *
     *
     * @param peerId1 the ID of the initiating peer
     * @param peerId2 the ID of the receiving peer
     */
    public static void peerHandshakeStarted(int peerId1, int peerId2) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] starts handshake with Peer [%d]", peerId1, peerId2);
                LogWriter.writeToFile("log_peer_"+peerId1+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer successfully completes a handshake process.
     *
     * @param peerId1 the ID of the initiating peer
     * @param peerId2 the ID of the receiving peer
     */
    public static void peerHandshakeCompleted(int peerId1, int peerId2) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] successfully completes handshake with Peer [%d]", peerId1, peerId2);
                LogWriter.writeToFile("log_peer_"+peerId1+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer is temporarily blocked by another peer.
     *
     * @param peerId1 the ID of the blocking peer
     * @param peerId2 the ID of the blocked peer
     */
    public static void peerBlocked(int peerId1, int peerId2) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] temporarily blocks Peer [%d]", peerId1, peerId2);
                LogWriter.writeToFile("log_peer_"+peerId1+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer unblocks another peer.
     *
     * @param peerId1 the ID of the unblocking peer
     * @param peerId2 the ID of the unblocked peer
     */
    public static void peerUnblocked(int peerId1, int peerId2) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] unblocks Peer [%d]", peerId1, peerId2);
                LogWriter.writeToFile("log_peer_"+peerId1+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer sends a data request to another peer.
     *
     * @param requesterPeerId the ID of the peer making the request
     * @param receiverPeerId the ID of the peer receiving the request
     */
    public static void dataRequestSent(int requesterPeerId, int receiverPeerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] sends a data request to Peer [%d]", requesterPeerId, receiverPeerId);
                LogWriter.writeToFile("log_peer_"+requesterPeerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // More logging methods can be added 

    public static void dataRequestSent(int requesterPeerId, int receiverPeerId, int pieceIndex) {
        try {
            String message = String.format("[Time]: Peer [%d] sends a data request for piece [%d] to Peer [%d]", 
                                            requesterPeerId, pieceIndex, receiverPeerId);
            LogWriter.writeToFile("log_peer_" + requesterPeerId + ".log", message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    

}
