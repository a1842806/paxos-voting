package council.voting.message;

import java.io.Serializable;

/**
 * Message class for communication between council members.
 * Implements Serializable for network transmission.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final int proposalNumber;
    private final String value;
    private final int senderId;
    private final int acceptedProposalNumber; // New field

    /**
     * Constructor for Message with acceptedProposalNumber (for PROMISE messages).
     *
     * @param type                  Type of the message
     * @param proposalNumber        Proposal number for the Paxos protocol
     * @param value                 Proposed value
     * @param senderId              ID of the sending council member
     * @param acceptedProposalNumber Highest accepted proposal number (for PROMISE messages)
     */
    public Message(MessageType type, int proposalNumber, String value, int senderId, int acceptedProposalNumber) {
        this.type = type;
        this.proposalNumber = proposalNumber;
        this.value = value;
        this.senderId = senderId;
        this.acceptedProposalNumber = acceptedProposalNumber;
    }

    /**
     * Constructor for Message without acceptedProposalNumber (for other message types).
     *
     * @param type           Type of the message
     * @param proposalNumber Proposal number for the Paxos protocol
     * @param value          Proposed value
     * @param senderId       ID of the sending council member
     */
    public Message(MessageType type, int proposalNumber, String value, int senderId) {
        this(type, proposalNumber, value, senderId, -1);
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public int getProposalNumber() {
        return proposalNumber;
    }

    public String getValue() {
        return value;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getAcceptedProposalNumber() {
        return acceptedProposalNumber;
    }

    @Override
    public String toString() {
        return String.format(
                "Message{type=%s, proposalNumber=%d, value='%s', senderId=%d, acceptedProposalNumber=%d}",
                type, proposalNumber, value, senderId, acceptedProposalNumber);
    }
}