import java.io.*;
import java.util.BitSet;
import logging.ConnectionEventLogger;
import logging.PeerEventLogger;
import errorhandling.P2PFileSharingException;
import errorhandling.P2PFileSharingException.ErrorType;

public class FileManager {
    private BitSet piecesHave;
    private final int pieceSize;
    private final int fileSize;
    private final String fileName;
    private byte[][] filePieces;
    private final int peerId; // Assuming peerId is passed to the FileManager for logging

    public FileManager(int fileSize, int pieceSize, String fileName, boolean hasFile, int peerId) throws P2PFileSharingException {
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.fileName = fileName;
        this.peerId = peerId;
        int numPieces = (fileSize + pieceSize - 1) / pieceSize;
        this.piecesHave = new BitSet(numPieces);
        this.filePieces = new byte[numPieces][];

        if (hasFile) {
            piecesHave.set(0, numPieces);
            try {
                loadFile();
            } catch (IOException e) {
                throw new P2PFileSharingException("Failed to load file: " + fileName, ErrorType.FILE_ERROR, e);
            }
        }
    }

    private void loadFile() throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
            for (int i = 0; i < filePieces.length; i++) {
                int pieceLength = Math.min(pieceSize, (int)file.length() - i * pieceSize);
                filePieces[i] = new byte[pieceLength];
                file.seek((long)i * pieceSize);
                file.readFully(filePieces[i]);
            }
            PeerEventLogger.downloadComplete(peerId);
        } catch (IOException e) {
            PeerEventLogger.logPeerCommunicationError(peerId, e);
            throw e;
        }
    }

    public synchronized byte[] getPiece(int index) throws P2PFileSharingException {
        if (index < 0 || index >= filePieces.length) {
            throw new P2PFileSharingException("Invalid piece index: " + index, ErrorType.FILE_ERROR);
        }
        return filePieces[index];
    }

    public synchronized void storePiece(int index, byte[] data) throws P2PFileSharingException {
        if (index < 0 || index >= filePieces.length) {
            throw new P2PFileSharingException("Invalid piece index: " + index, ErrorType.FILE_ERROR);
        }
        filePieces[index] = data;
        piecesHave.set(index);
        if (piecesHave.cardinality() == filePieces.length) {
            reassembleFile();
        }
    }

    private void reassembleFile() throws P2PFileSharingException {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            for (byte[] piece : filePieces) {
                if (piece != null) {
                    fos.write(piece);
                } else {
                    throw new P2PFileSharingException("Missing file piece during reassembly", ErrorType.FILE_ERROR);
                }
            }
            PeerEventLogger.downloadComplete(peerId);
        } catch (IOException e) {
            throw new P2PFileSharingException("Failed to reassemble file: " + fileName, ErrorType.FILE_ERROR, e);
        }
    }

    public synchronized BitSet getBitfield() {
        return (BitSet) piecesHave.clone();
    }

    public synchronized void updateHave(int pieceIndex) {
        piecesHave.set(pieceIndex);
    }

    public synchronized void updateBitfield(BitSet bitfield) {
        piecesHave.or(bitfield);
    }

    public synchronized void sendPiece(int pieceIndex, DataOutputStream out) throws P2PFileSharingException {
        if (!piecesHave.get(pieceIndex)) {
            return;
        }

        byte[] pieceData = getPiece(pieceIndex);
        if (pieceData != null) {
            try {
                out.writeInt(pieceData.length);
                out.write(pieceData);
            } catch (IOException e) {
                throw new P2PFileSharingException("Failed to send piece: " + pieceIndex, ErrorType.CONNECTION_ERROR, e);
            }
        }
    }
}
