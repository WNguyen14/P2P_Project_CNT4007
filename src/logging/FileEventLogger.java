
package logging;

import java.io.IOException;

/**
 * FileEventLogger class for logging various file-related events.
 * This class provides specialized methods to log events related to file interactions in the P2P network.
 */
public class FileEventLogger {

    /**
     * Logs a message when a peer downloads a piece of a file.
     *
     * @param peerId the ID of the peer that downloaded the piece
     * @param pieceIndex the index of the downloaded piece
     */
    public static void filePieceDownloaded(int peerId, int pieceIndex) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has downloaded the piece [%d]", peerId, pieceIndex);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer finishes downloading the complete file.
     *
     * @param peerId the ID of the peer that completed the download
     */
    public static void fileDownloadComplete(int peerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has downloaded the complete file", peerId);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer starts uploading a file.
     *
     * @param peerId the ID of the peer that started uploading
     */
    public static void fileUploadStarted(int peerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has started uploading the file", peerId);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer successfully uploads a piece of a file.
     *
     * @param peerId the ID of the peer that uploaded the piece
     * @param receiverPeerId the ID of the peer that received the piece
     * @param pieceIndex the index of the uploaded piece
     */
    public static void filePieceUploaded(int peerId, int receiverPeerId, int pieceIndex) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has uploaded the piece [%d] to Peer [%d]", peerId, pieceIndex, receiverPeerId);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer successfully uploads the complete file.
     *
     * @param peerId the ID of the peer that completed the upload
     */
    public static void fileUploadComplete(int peerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has completed uploading the file", peerId);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer deletes a file.
     *
     * @param peerId the ID of the peer that deleted the file
     */
    public static void fileDeleted(int peerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has deleted the file", peerId);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a peer starts a file request.
     *
     * @param requesterPeerId the ID of the peer making the request
     * @param receiverPeerId the ID of the peer receiving the request
     */
    public static void fileRequestStarted(int requesterPeerId, int receiverPeerId) {
        if (Logger.getCurrentLogLevel().ordinal() <= Logger.LogLevel.INFO.ordinal()) {
            try {
                String message = String.format("[Time]: Peer [%d] has started a file request to Peer [%d]", requesterPeerId, receiverPeerId);
                LogWriter.writeToFile("info.log", message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
