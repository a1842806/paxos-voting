package council.voting.exception;

/**
 * Custom exception class for handling voting-related errors in the council voting system.
 * This exception is thrown when errors occur during network communication, proposal processing,
 * or any other voting-related operations.
 */
public class VotingException extends RuntimeException {
    
    /**
     * Constructs a new voting exception with the specified detail message.
     * 
     * @param message The detail message explaining the error
     */
    public VotingException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new voting exception with the specified detail message and cause.
     * 
     * @param message The detail message explaining the error
     * @param cause The cause of the error
     */
    public VotingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new voting exception with the specified cause.
     * 
     * @param cause The cause of the error
     */
    public VotingException(Throwable cause) {
        super(cause);
    }
}