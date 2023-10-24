import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * LogWriter class for handling writing logs to files.
 */
public class LogWriter {

    /**
     * Writes a message to a log file.
     *
     * @param fileName the name of the log file
     * @param message  the log message
     * @throws IOException if an I/O error occurs
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
}
