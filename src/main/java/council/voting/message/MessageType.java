package council.voting.message;

/**
 * Enum representing different types of messages in the Paxos protocol.
 */
public enum MessageType {
    HANDSHAKE, // New message type for initial connection setup
    PREPARE,   // First phase of Paxos - Prepare request
    PROMISE,   // Response to PREPARE if accepted
    ACCEPT,    // Second phase of Paxos - Accept request
    ACCEPTED,  // Response to ACCEPT if accepted
    REJECT     // Response to either PREPARE or ACCEPT if rejected
}