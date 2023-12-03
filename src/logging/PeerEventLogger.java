package logging;

import java.io.IOException;

/**
 * PeerEventLogger class for logging various peer-related events.
 * This class provides specialized methods to log events related to peer actions in the P2P network.
 */
public class PeerEventLogger {

    /**
     * Logs a message when a peer changes its preferred neighbors.
     *
     * @param peerId the ID of the peer making the change
     * @param preferredNeighbors comma-separated string of preferred neighbor IDs
     */
    public static void preferredNeighborsChanged(int peerId, String preferredNeighbors) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has the preferred neighbors [%s]", peerId, preferredNeighbors);
                LogWriter.writeToFile("log_peer_"+peerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer changes its optimistically unchoked neighbor.
     *
     * @param peerId the ID of the peer making the change
     * @param optimisticNeighbor the ID of the optimistically unchoked neighbor
     */
    public static void optimisticNeighborChanged(int peerId, int optimisticNeighbor) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has the optimistically unchoked neighbor [%d]", peerId, optimisticNeighbor);
                LogWriter.writeToFile("log_peer_"+peerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer receives a 'have' message.
     *
     * @param receiverPeerId the ID of the receiving peer
     * @param senderPeerId the ID of the sending peer
     * @param pieceIndex the piece index received
     */
    public static void receivedHaveMessage(int receiverPeerId, int senderPeerId, int pieceIndex) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] received the 'have' message from [%d] for the piece [%d]", receiverPeerId, senderPeerId, pieceIndex);
                LogWriter.writeToFile("log_peer_"+receiverPeerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer receives an 'interested' message.
     *
     * @param receiverPeerId the ID of the receiving peer
     * @param senderPeerId the ID of the sending peer
     */
    public static void receivedInterestedMessage(int receiverPeerId, int senderPeerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] received the 'interested' message from [%d]", receiverPeerId, senderPeerId);
                LogWriter.writeToFile("log_peer_"+receiverPeerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer receives a 'not interested' message.
     *
     * @param receiverPeerId the ID of the receiving peer
     * @param senderPeerId the ID of the sending peer
     */
    public static void receivedNotInterestedMessage(int receiverPeerId, int senderPeerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] received the 'not interested' message from [%d]", receiverPeerId, senderPeerId);
                LogWriter.writeToFile("log_peer_"+receiverPeerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer finishes downloading a piece.
     *
     * @param downloaderPeerId the ID of the downloading peer
     * @param senderPeerId the ID of the sending peer
     * @param pieceIndex the index of the downloaded piece
     * @param numberOfPieces the updated number of pieces the downloading peer has
     */
    public static void pieceDownloaded(int downloaderPeerId, int senderPeerId, int pieceIndex, int numberOfPieces) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has downloaded the piece [%d] from [%d]. Now the number of pieces it has is [%d]", downloaderPeerId, pieceIndex, senderPeerId, numberOfPieces);
                LogWriter.writeToFile("log_peer_"+downloaderPeerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer downloads the complete file.
     *
     * @param peerId the ID of the peer that completed the download
     */
    public static void downloadComplete(int peerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has downloaded the complete file", peerId);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //logs a message when a peer is choked
    public static void peerChoked(int peerId, int neighborId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] is choked by [%d]", peerId, neighborId);
                LogWriter.writeToFile("log_peer_"+peerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //log message when peer unchoked
    public static void peerUnchoked(int peerId, int neighborId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] is unchoked by [%d]", peerId, neighborId);
                LogWriter.writeToFile("log_peer_"+peerId+".log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void logPeerCommunicationError(int peerId, Exception e) {
        try {
            String message = String.format("[Time]: Communication error in Peer [%d]: %s", peerId, e.getMessage());
            LogWriter.writeToFile("log_peer_" + peerId + ".log", message);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
}
