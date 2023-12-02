import java.util.BitSet;

public class FileManager {

    private BitSet piecesHave; // Tracks which pieces this peer has
    private final int pieceSize;
    private final int fileSize;
    private final String fileName;
    private byte[][] filePieces;

    public FileManager(int fileSize, int pieceSize, String fileName, boolean hasFile) {
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
        this.fileName = fileName;
        int numPieces = (fileSize + pieceSize - 1) / pieceSize;
        this.piecesHave = new BitSet(numPieces);

        if (hasFile) {
            piecesHave.set(0, numPieces); // If the peer has the file, set all bits to true.
            // Read the file and split it into pieces.
            // Each piece would be stored in 'filePieces'.
        } else {
            // Initialize 'filePieces' to store incoming data.
            filePieces = new byte[numPieces][];
        }
    }

    // Call this method to get a piece of the file
    public byte[] getPiece(int index) {
        if (index < 0 || index >= filePieces.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Pieces: " + filePieces.length);
        }
        return filePieces[index];
    }

    // Call this method to store a piece of the file
    public void storePiece(int index, byte[] data) {
        if (index < 0 || index >= filePieces.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Pieces: " + filePieces.length);
        }
        filePieces[index] = data;
        piecesHave.set(index);
        // If all pieces have been received, you can reassemble the file.
        if (piecesHave.cardinality() == filePieces.length) {
            reassembleFile();
        }
    }

    private void reassembleFile() {
        // Logic to reassemble the file from the pieces stored in 'filePieces'.
        // This would involve writing the data back to disk.
    }

    // Call this to get the bitfield representing the pieces this peer has
    public BitSet getBitfield() {
        return (BitSet) piecesHave.clone();
    }

    // Add other methods as needed for your implementation...
}
