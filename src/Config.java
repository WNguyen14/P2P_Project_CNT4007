import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;
import logging.Logger;
import errorhandling.P2PFileSharingException;

/**
 * This class is responsible for parsing and holding configuration information 
 * from the Common.cfg file in a peer-to-peer file sharing application.
 */
public class Config {

    private final int numberOfPreferredNeighbors;
    private final int unchokingInterval;
    private final int optimisticUnchokingInterval;
    private final String configFileName;
    private final int fileSize;
    private final int pieceSize;

    /**
     * Constructor that reads and parses the configuration file.
     * 
     * @param fileName The path to the configuration file.
     * @throws P2PFileSharingException If the file is not found or there's an error in reading the file.
     */
    public Config(String fileName) throws P2PFileSharingException {
        try {
            Scanner in = new Scanner(new FileReader(fileName));

            this.numberOfPreferredNeighbors = Integer.parseInt(in.nextLine().split(" ")[1]);
            this.unchokingInterval = Integer.parseInt(in.nextLine().split(" ")[1]);
            this.optimisticUnchokingInterval = Integer.parseInt(in.nextLine().split(" ")[1]);
            this.configFileName = in.nextLine().split(" ")[1];
            this.fileSize = Integer.parseInt(in.nextLine().split(" ")[1]);
            this.pieceSize = Integer.parseInt(in.nextLine().split(" ")[1]);

            in.close();

            // Log that the configuration file was successfully read.
            Logger.info("Configuration file %s loaded successfully.", fileName);
        } catch (FileNotFoundException e) {
            Logger.error("Configuration file %s not found: %s", fileName, e.getMessage());
            throw new P2PFileSharingException("Config file not found", 
                    P2PFileSharingException.ErrorType.FILE_ERROR, e);
        }
    }

    // Getters for the configuration values
    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }
}
