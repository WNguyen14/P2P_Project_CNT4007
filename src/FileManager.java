import java.io.*;
import java.util.BitSet;

public class FileManager {
    private BitSet piecesHave; // Tracks which pieces this peer has
    private final int pieceSize;
    private final int fileSize;
    private final String fileName;
    private byte[][] filePieces; // Stores the file pieces

    // Constructor to set up the FileManager.
    public FileManager(int fileSize, int pieceSize, String fileName, boolean hasFile) {
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.fileName = fileName;
        int numPieces = (fileSize + pieceSize - 1) / pieceSize; // Calculate the number of pieces
        this.piecesHave = new BitSet(numPieces);
        this.filePieces = new byte[numPieces][];

        if (hasFile) {
            piecesHave.set(0, numPieces); // If the peer has the file, set all bits to true.
            loadFile(); // Load the file into memory if the peer has it
        }
    }

    // Loads the file into the filePieces array.
    private void loadFile() {
        try (RandomAccessFile file = new RandomAccessFile(fileName, "r")) {
            for (int i = 0; i < filePieces.length; i++) {
                int pieceLength = Math.min(pieceSize, (int)file.length() - i * pieceSize);
                filePieces[i] = new byte[pieceLength];
                file.seek((long)i * pieceSize);
                file.readFully(filePieces[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gets a piece of the file by index.
    public synchronized byte[] getPiece(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= filePieces.length) {
            throw new IndexOutOfBoundsException("Invalid piece index: " + index);
        }
        return filePieces[index];
    }

    // Stores a piece of the file by index.
    public synchronized void storePiece(int index, byte[] data) throws IndexOutOfBoundsException {
        if (index < 0 || index >= filePieces.length) {
            throw new IndexOutOfBoundsException("Invalid piece index: " + index);
        }
        filePieces[index] = data;
        piecesHave.set(index);
        if (piecesHave.cardinality() == filePieces.length) {
            reassembleFile();
        }
    }

    // Reassembles the file from pieces when all pieces have been received.
    private void reassembleFile() {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            for (byte[] piece : filePieces) {
                if (piece != null) {
                    fos.write(piece);
                } else {
                    System.out.println("Missing file piece during reassembly");
                    return;
                }
            }
            System.out.println("File reassembly complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gets the bitfield representing the pieces this peer has.
    public synchronized BitSet getBitfield() {
        return (BitSet) piecesHave.clone();
    }

    // Updates the bitfield when a 'have' message is received.
    public synchronized void updateHave(int pieceIndex) {
        piecesHave.set(pieceIndex);
    }

    // Updates the bitfield when a 'bitfield' message is received.
    public synchronized void updateBitfield(BitSet bitfield) {
        piecesHave.or(bitfield);
    }

    // Sends a piece to the peer over the given DataOutputStream.
    public synchronized void sendPiece(int pieceIndex, DataOutputStream out) throws IOException {
        if (!piecesHave.get(pieceIndex)) {
            System.out.println("Do not have piece " + pieceIndex);
            return; // Do not have the piece.
        }

        byte[] pieceData = getPiece(pieceIndex);
        if (pieceData != null) {
            out.writeInt(pieceData.length);
            out.write(pieceData);
        }
    }
}
