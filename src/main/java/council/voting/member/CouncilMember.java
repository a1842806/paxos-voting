// CouncilMember.java

package council.voting.member;

import council.voting.message.Message;
import council.voting.message.MessageType;
import council.voting.network.Connection;
import council.voting.network.NetworkManager;
import council.voting.exception.VotingException;

import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Represents a council member in the Adelaide Suburbs Council.
 * Implements the complete Paxos consensus protocol including proposer, acceptor, and learner roles.
 */
public class CouncilMember implements Runnable {
    // Logger for debugging
    private static final Logger LOGGER = Logger.getLogger(CouncilMember.class.getName());

    private final int memberId; // Unique ID for this member
    @SuppressWarnings("unused")
    private final String name; // Name of this member
    @SuppressWarnings("unused")
    private final int port; // Port for this member
    private final Map<Integer, String> memberAddresses; // Map of member IDs to hostnames/IPs
    private final Map<Integer, Integer> memberPorts;    // Map of member IDs to ports
    private final ResponseProfile profile; // Response profile for this member
    private final PaxosState paxosState; // State for the Paxos protocol
    private final NetworkManager networkManager; // Manages network operations

    private final ExecutorService executorService; // For handling incoming messages
    private final Map<Integer, Connection> connections; // Connections to other members

    private volatile boolean isOnline = true; // Flag to control the member's lifecycle

    // Constructor
    public CouncilMember(int memberId, String name, int port,
                         Map<Integer, String> memberAddresses, Map<Integer, Integer> memberPorts,
                         ResponseProfile profile) {
        this.memberId = memberId;
        this.name = name;
        this.port = port;
        this.memberAddresses = memberAddresses;
        this.memberPorts = memberPorts;
        this.profile = profile;
        this.paxosState = new PaxosState();
        this.networkManager = new NetworkManager(port);

        this.executorService = Executors.newCachedThreadPool();
        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        try {
            // Start the server to accept incoming connections
            networkManager.startServer();
            executorService.submit(this::acceptConnections);

            // Connect to other members
            connectToMembers();

            // Start message handling for existing connections
            for (Connection connection : connections.values()) {
                executorService.submit(() -> handleIncomingMessages(connection));
            }

        } catch (VotingException e) {
            LOGGER.log(Level.SEVERE, "Failed to start CouncilMember", e);
        }
    }

    /**
     * Proposes a value for consensus.
     *
     * @param value The value to propose (e.g., the name of the president)
     * @return true if consensus was reached, false otherwise
     */
    public boolean proposeValue(String value) {
        if (!isOnline) {
            LOGGER.warning("Member is offline");
            return false;
        }

        int proposalNumber = paxosState.generateProposalNumber(memberId);

        try {
            simulateDelay();

            // Phase 1a: Send Prepare requests to all acceptors
            Message prepareMsg = new Message(MessageType.PREPARE, proposalNumber, null, memberId);
            Map<Integer, Message> promises = sendPrepareRequests(prepareMsg);

            // Handle Prepare responses
            if (promises.size() < (memberAddresses.size() / 2) + 1) {
                LOGGER.warning("Did not receive majority of promises");
                return false;
            }

            // Choose the highest-numbered accepted value, if any
            String acceptedValue = value;
            int highestProposalNum = -1;
            for (Message promise : promises.values()) {
                if (promise.getAcceptedProposalNumber() > highestProposalNum) {
                    highestProposalNum = promise.getAcceptedProposalNumber();
                    acceptedValue = promise.getValue() != null ? promise.getValue() : value;
                }
            }

            // Phase 2a: Send Accept requests with the chosen value
            Message acceptMsg = new Message(MessageType.ACCEPT, proposalNumber, acceptedValue, memberId);
            Map<Integer, Message> acceptResponses = sendAcceptRequests(acceptMsg);

            // Handle Accept responses
            if (acceptResponses.size() < (memberAddresses.size() / 2) + 1) {
                LOGGER.warning("Did not receive majority of acceptances");
                return false;
            }

            // Learned value
            LOGGER.info("Consensus reached on value: " + acceptedValue);

            return true;

        } catch (VotingException e) {
            LOGGER.log(Level.WARNING, "Proposal failed", e);
            return false;
        }
    }

    /**
     * Sends Prepare requests to all acceptors and collects promises.
     */
    private Map<Integer, Message> sendPrepareRequests(Message prepareMsg) {
        Map<Integer, Future<Message>> futurePromises = new HashMap<>();

        for (Map.Entry<Integer, Connection> entry : connections.entrySet()) {
            int memberId = entry.getKey();
            Connection connection = entry.getValue();

            Future<Message> future = executorService.submit(() -> {
                try {
                    simulateDelay();
                    connection.sendMessage(prepareMsg);
                    Message response = connection.receiveMessage();
                    if (response.getType() == MessageType.PROMISE) {
                        return response;
                    }
                } catch (VotingException e) {
                    LOGGER.log(Level.WARNING, "Failed to get promise from member " + memberId, e);
                }
                return null;
            });
            futurePromises.put(memberId, future);
        }

        // Collect promises
        Map<Integer, Message> promises = new HashMap<>();
        for (Map.Entry<Integer, Future<Message>> entry : futurePromises.entrySet()) {
            try {
                Message promise = entry.getValue().get(profile.getIndividualMessageTimeout(), TimeUnit.MILLISECONDS);
                if (promise != null) {
                    promises.put(entry.getKey(), promise);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.log(Level.WARNING, "Timed out waiting for promise from member " + entry.getKey(), e);
            }
        }
        return promises;
    }

    /**
     * Sends Accept requests to all acceptors and collects acceptances.
     */
    private Map<Integer, Message> sendAcceptRequests(Message acceptMsg) {
        Map<Integer, Future<Message>> futureAcceptances = new HashMap<>();

        for (Map.Entry<Integer, Connection> entry : connections.entrySet()) {
            int memberId = entry.getKey();
            Connection connection = entry.getValue();

            Future<Message> future = executorService.submit(() -> {
                try {
                    simulateDelay();
                    connection.sendMessage(acceptMsg);
                    Message response = connection.receiveMessage();
                    if (response.getType() == MessageType.ACCEPTED) {
                        return response;
                    }
                } catch (VotingException e) {
                    LOGGER.log(Level.WARNING, "Failed to get acceptance from member " + memberId, e);
                }
                return null;
            });
            futureAcceptances.put(memberId, future);
        }

        // Collect acceptances
        Map<Integer, Message> acceptances = new HashMap<>();
        for (Map.Entry<Integer, Future<Message>> entry : futureAcceptances.entrySet()) {
            try {
                Message acceptance = entry.getValue().get(profile.getIndividualMessageTimeout(), TimeUnit.MILLISECONDS);
                if (acceptance != null) {
                    acceptances.put(entry.getKey(), acceptance);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.log(Level.WARNING, "Timed out waiting for acceptance from member " + entry.getKey(), e);
            }
        }
        return acceptances;
    }

    /**
     * Connects to other council members.
     */
    private void connectToMembers() {
        for (Map.Entry<Integer, String> entry : memberAddresses.entrySet()) {
            int otherMemberId = entry.getKey();
            String host = entry.getValue();
            int port = memberPorts.get(otherMemberId);

            if (otherMemberId == memberId) continue; // Skip self

            try {
                Socket socket = networkManager.connect(host, port);
                Connection connection = new Connection(socket);
                // Send handshake message with our member ID
                Message handshakeMsg = new Message(MessageType.HANDSHAKE, 0, null, memberId);
                connection.sendMessage(handshakeMsg);

                // Receive handshake message with their member ID
                Message response = connection.receiveMessage();
                int remoteMemberId = response.getSenderId();
                if (response.getType() == MessageType.HANDSHAKE) {
                    connections.put(remoteMemberId, connection);
                    LOGGER.info("Connected to member " + remoteMemberId);
                    executorService.submit(() -> handleIncomingMessages(connection));
                } else {
                    throw new VotingException("Expected HANDSHAKE message");
                }

            } catch (VotingException e) {
                LOGGER.log(Level.WARNING, "Failed to connect to member " + otherMemberId, e);
            }
        }
    }

    /**
     * Accepts incoming connections from other council members.
     */
    private void acceptConnections() {
        while (isOnline) {
            try {
                Socket socket = networkManager.acceptConnection();
                Connection connection = new Connection(socket);

                // Receive handshake message with their member ID
                Message handshakeMsg = connection.receiveMessage();
                int remoteMemberId = handshakeMsg.getSenderId();
                if (handshakeMsg.getType() == MessageType.HANDSHAKE) {
                    // Send handshake response with our member ID
                    Message response = new Message(MessageType.HANDSHAKE, 0, null, memberId);
                    connection.sendMessage(response);

                    connections.put(remoteMemberId, connection);
                    LOGGER.info("Accepted connection from member " + remoteMemberId);
                    executorService.submit(() -> handleIncomingMessages(connection));
                } else {
                    throw new VotingException("Expected HANDSHAKE message");
                }

            } catch (VotingException e) {
                if (isOnline) {
                    LOGGER.log(Level.WARNING, "Failed to accept connection", e);
                }
            }
        }
    }

    /**
     * Handles incoming messages from a specific connection.
     */
    private void handleIncomingMessages(Connection connection) {
        try {
            while (isOnline && !connection.getSocket().isClosed()) {
                Message message = connection.receiveMessage();
                if (message != null) {
                    processMessage(message, connection);
                }
            }
        } catch (VotingException e) {
            LOGGER.log(Level.WARNING, "Connection lost", e);
        } finally {
            // Remove the connection from the map
            connections.values().remove(connection);
            connection.close();
        }
    }

    /**
     * Processes incoming messages based on their type.
     */
    private void processMessage(Message message, Connection connection) {
        switch (message.getType()) {
            case PREPARE:
                handlePrepareRequest(message, connection);
                break;
            case ACCEPT:
                handleAcceptRequest(message, connection);
                break;
            // You can add more cases for other message types if needed
            default:
                LOGGER.warning("Received unknown message type");
        }
    }

    /**
     * Handles incoming PREPARE requests (Phase 1b).
     */
    private void handlePrepareRequest(Message message, Connection connection) {
        simulateDelay();
        int incomingProposalNumber = message.getProposalNumber();
        synchronized (paxosState) {
            if (incomingProposalNumber > paxosState.getPromisedProposalNumber()) {
                paxosState.setPromisedProposalNumber(incomingProposalNumber);

                // Send PROMISE response with any previously accepted proposal
                Message promise = new Message(
                        MessageType.PROMISE,
                        incomingProposalNumber,
                        paxosState.getAcceptedValue(),
                        memberId,
                        paxosState.getAcceptedProposalNumber()
                );
                connection.sendMessage(promise);
            } else {
                // Send REJECT response
                Message reject = new Message(
                        MessageType.REJECT,
                        paxosState.getPromisedProposalNumber(),
                        null,
                        memberId
                );
                connection.sendMessage(reject);
            }
        }
    }

    /**
     * Handles incoming ACCEPT requests (Phase 2b).
     */
    private void handleAcceptRequest(Message message, Connection connection) {
        simulateDelay();
        int incomingProposalNumber = message.getProposalNumber();
        String value = message.getValue();
        synchronized (paxosState) {
            if (incomingProposalNumber >= paxosState.getPromisedProposalNumber()) {
                paxosState.setPromisedProposalNumber(incomingProposalNumber);
                paxosState.setAcceptedProposalNumber(incomingProposalNumber);
                paxosState.setAcceptedValue(value);

                // Send ACCEPTED response
                Message accepted = new Message(
                        MessageType.ACCEPTED,
                        incomingProposalNumber,
                        value,
                        memberId
                );
                connection.sendMessage(accepted);
            } else {
                // Send REJECT response
                Message reject = new Message(
                        MessageType.REJECT,
                        paxosState.getPromisedProposalNumber(),
                        null,
                        memberId
                );
                connection.sendMessage(reject);
            }
        }
    }

    /**
     * Simulates delays and message loss based on the member's profile.
     */
    private void simulateDelay() {
        try {
            // Simulate network delay
            if (profile.getMaxDelay() > 0) {
                Thread.sleep(profile.getRandomDelay());
            }

            // Simulate message loss or unresponsiveness
            if (profile.shouldDropMessage()) {
                throw new VotingException("Simulated message drop");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VotingException("Delay simulation interrupted", e);
        }
    }

    /**
     * Shuts down the council member and cleans up resources.
     */
    public void shutdown() {
        isOnline = false;
        executorService.shutdownNow();
        networkManager.shutdown();

        // Close all connections
        for (Connection connection : connections.values()) {
            connection.close();
        }
        connections.clear();
    }

    /**
     * Main method to start a council member.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java CouncilMember <memberId> <port> [propose]");
            System.exit(1);
        }
    
        int memberId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        boolean shouldPropose = args.length > 2 && args[2].equalsIgnoreCase("propose");
    
        // Create member addresses and ports
        Map<Integer, String> addresses = new HashMap<>();
        Map<Integer, Integer> ports = new HashMap<>();
        // Assume localhost and ports starting from 8001
        for (int i = 1; i <= 9; i++) {
            addresses.put(i, "localhost");
            ports.put(i, 8000 + i); // Ports 8001 to 8009
        }
    
        // Define profiles for each member based on memberId
        ResponseProfile profile;
        switch (memberId) {
            case 1:
                profile = ResponseProfile.IMMEDIATE; // M1
                break;
            case 2:
                profile = ResponseProfile.INTERMITTENT; // M2
                break;
            case 3:
                profile = ResponseProfile.UNRELIABLE; // M3
                break;
            default:
                profile = ResponseProfile.NORMAL; // M4-M9
        }
    
        CouncilMember member = new CouncilMember(
                memberId,
                "Member" + memberId,
                port,
                addresses,
                ports,
                profile
        );
    
        new Thread(member).start();
    
        if (shouldPropose) {
            // Wait a moment to ensure connections are established
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Main thread interrupted");
            }
            member.proposeValue("Value from Member " + memberId);
        }
    }
}