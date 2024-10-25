package council.voting.member;

/**
 * Encapsulates the Paxos protocol state for a council member.
 */
public class PaxosState {
    private int proposalNumber;
    private int promisedProposalNumber;       // New field
    private int acceptedProposalNumber;
    private String acceptedValue;

    public PaxosState() {
        this.proposalNumber = 0;
        this.promisedProposalNumber = -1;   
        this.acceptedProposalNumber = -1;
        this.acceptedValue = null;
    }

    public synchronized int generateProposalNumber(int memberId) {
        return (++proposalNumber << 4) | (memberId & 0xF);
    }

    public synchronized void updateAcceptedProposal(int proposalNumber, String value) {
        if (proposalNumber >= this.acceptedProposalNumber) {
            this.acceptedProposalNumber = proposalNumber;
            this.acceptedValue = value;
        }
    }

    public synchronized int getPromisedProposalNumber() {
        return promisedProposalNumber;
    }

    public synchronized void setPromisedProposalNumber(int proposalNumber) {
        this.promisedProposalNumber = proposalNumber;
    }

    public synchronized int getAcceptedProposalNumber() {
        return acceptedProposalNumber;
    }

    public synchronized String getAcceptedValue() {
        return acceptedValue;
    }

    public synchronized void setAcceptedProposalNumber(int proposalNumber) {
        this.acceptedProposalNumber = proposalNumber;
    }

    public synchronized void setAcceptedValue(String value) {
        this.acceptedValue = value;
    }

    public synchronized void reset() {
        this.proposalNumber = 0;
        this.promisedProposalNumber = -1;
        this.acceptedProposalNumber = -1;
        this.acceptedValue = null;
    }
}