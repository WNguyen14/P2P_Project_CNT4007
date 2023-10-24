package logging;

import java.io.IOException;

/**
 * Logger class for handling various types of logging.
 * This class provides methods to log messages at different levels of severity.
 * It can log messages at INFO, DEBUG, ERROR, and WARN levels.
 */
public class Logger {

    /**
     * Enum to represent various types of logging levels.
     */
    public enum LogLevel {
        INFO, DEBUG, ERROR, WARN
    }

    // Variable to hold the current log level for the application.
    private static LogLevel currentLogLevel = LogLevel.INFO;

    /**
     * Sets the logging level for the application.
     *
     * @param level the LogLevel to set
     */
    public static void setLogLevel(LogLevel level) {
        currentLogLevel = level;
    }

    /**
     * Returns the current log level for the application.
     *
     * @return current LogLevel
     */
    public static LogLevel getCurrentLogLevel() {
        return currentLogLevel;
    }

    /**
     * Logs a message when a new peer is connected.
     *
     * @param peerId the ID of the peer that connected
     */
    public static void peerConnected(int peerId) {
        if (currentLogLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            try {
                LogWriter.writeToFile("info.log", "New peer connected: " + peerId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message when a file is downloaded.
     *
     * @param fileName the name of the downloaded file
     */
    public static void fileDownloaded(String fileName) {
        if (currentLogLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            try {
                LogWriter.writeToFile("info.log", "File downloaded: " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Logs a message at the INFO level.
     *
     * @param message the message to log
     * @param args optional arguments to format into the message
     */
    public static void info(String message, Object... args) {
        if (currentLogLevel.ordinal() <= LogLevel.INFO.ordinal()) {
            log("INFO", message, args);
        }
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param message the message to log
     * @param args optional arguments to format into the message
     */
    public static void debug(String message, Object... args) {
        if (currentLogLevel.ordinal() <= LogLevel.DEBUG.ordinal()) {
            log("DEBUG", message, args);
        }
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param message the message to log
     * @param args optional arguments to format into the message
     */
    public static void error(String message, Object... args) {
        if (currentLogLevel.ordinal() <= LogLevel.ERROR.ordinal()) {
            log("ERROR", message, args);
        }
    }

    /**
     * Logs a message at the WARN level.
     *
     * @param message the message to log
     * @param args optional arguments to format into the message
     */
    public static void warn(String message, Object... args) {
        if (currentLogLevel.ordinal() <= LogLevel.WARN.ordinal()) {
            log("WARN", message, args);
        }
    }

    /**
     * Internal method to perform the logging. Formats the log message and prints it to System.out.
     *
     * @param level the log level as a String
     * @param message the message to log
     * @param args optional arguments to format into the message
     */
    private static void log(String level, String message, Object... args) {
        // Get the current time for the timestamp
        String timestamp = java.time.LocalDateTime.now().toString();

        // Format the message with the optional arguments
        String formattedMessage = String.format(message, args);

        // Construct the log entry
        String logEntry = String.format("[%s] [%s]: %s", timestamp, level, formattedMessage);

        // Output the log entry
        System.out.println(logEntry);
    }
}
