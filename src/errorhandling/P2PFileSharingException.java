package errorhandling;


public class P2PFileSharingException extends Exception {

    /**
     * Enumeration to categorize the type of errors that can occur
     * in the P2P File Sharing Project.
     * - CONNECTION_ERROR: Errors related to TCP connection between peers
     * - HANDSHAKE_ERROR: Errors related to the handshake process between peers
     * - MESSAGE_ERROR: Errors related to the types and formats of messages exchanged between peers
     * - FILE_ERROR: Errors related to file reading, writing, or piece handling
     * - LOG_ERROR: Errors related to logging activities of peers
     */
    public enum ErrorType {
        CONNECTION_ERROR,
        HANDSHAKE_ERROR,
        MESSAGE_ERROR,
        FILE_ERROR,
        LOG_ERROR
    }

    // Variable to hold the type of error
    private ErrorType errorType;

    /**
     * Default constructor.
     */
    public P2PFileSharingException() {
        super();
    }

    /**
     * Constructor with a custom error message and the type of error.
     * @param message Custom error message
     * @param errorType Type of error
     */
    public P2PFileSharingException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * Constructor with a custom error message, the type of error, 
     * and the root cause of the exception.
     * @param message Custom error message
     * @param errorType Type of error
     * @param cause Root cause of the exception
     */
    public P2PFileSharingException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    

    /**
     * Returns the type of the error.
     * @return Type of the error as defined in ErrorType enum
     */
    public ErrorType getErrorType() {
        return this.errorType;
    }

    /**
     * Sets the type of the error.
     * @param errorType Type of error to set
     */
    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    /**
     * Generates a string representation of the exception.
     * @return String representation of the exception
     */
    @Override
    public String toString() {
        return "P2PFileSharingException: Type - " + errorType + ", Message - " + getMessage();
    }

    /**
     * Prints the stack trace along with the type of the error.
     */
    @Override
    public void printStackTrace() {
        System.err.println(this.toString());
        super.printStackTrace();
    }

    
}