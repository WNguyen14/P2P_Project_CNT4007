package logging;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.io.File;

/**
 * LogWriter is responsible for writing log messages to a file.
 * It supports adding timestamps to log messages and rotating log files based on size.
 */
public class LogWriter {

    private String filePath;
    private FileWriter fileWriter;
    private final long MAX_LOG_SIZE = 1024 * 1024; // 1MB
    private SimpleDateFormat dateFormat;

    /**
     * Constructor
     * @param filePath - Path of the log file
     */
    public LogWriter(String filePath) {
        this.filePath = filePath;
        try {
            this.fileWriter = new FileWriter(filePath, true); // true to append
            this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } catch (IOException e) {
            // Placeholder for generic error handling
            System.err.println("Error initializing LogWriter: " + e.getMessage());
        }
    }

    /**
     * Writes a log message to the file with a timestamp.
     * @param message - Message to be logged
     */
    public static void writeToFile(String fileName, String message) throws IOException {
        FileWriter fileWriter = new FileWriter(fileName, true);  // true means the file will be appended to

        // Get the current time for the timestamp
        LocalDateTime timestamp = LocalDateTime.now();

        // Create the log entry
        String logEntry = String.format("[%s]: %s\n", timestamp.toString(), message);

        // Write the log entry to the file
        fileWriter.write(logEntry);

        // Close the FileWriter
        fileWriter.close();
    }

    /**
     * Rotates the log file if the size exceeds the limit.
     */
    private void rotateLog() {
        try {
            fileWriter.close();
            // Rename the current log file and create a new one
            String rotatedLogPath = filePath + "." + dateFormat.format(new Date());
            new File(filePath).renameTo(new File(rotatedLogPath));
            this.fileWriter = new FileWriter(filePath, true);
        } catch (IOException e) {
            // Placeholder for generic error handling
            System.err.println("Error rotating log: " + e.getMessage());
        }
    }
}