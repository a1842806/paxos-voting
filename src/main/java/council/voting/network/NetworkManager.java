package council.voting.network;

import council.voting.exception.VotingException;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handles network communication between council members.
 * Manages server socket operations and provides methods to connect to other members.
 */
public class NetworkManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkManager.class.getName());
    private static final int CONNECTION_TIMEOUT = 5000; // 5 seconds

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public NetworkManager(int port) {
        this.port = port;
        this.running = true;
    }

    /**
     * Starts the server socket to accept incoming connections.
     * @throws VotingException if socket creation fails.
     */
    public void startServer() throws VotingException {
        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("Server started on port " + port);
        } catch (IOException e) {
            throw new VotingException("Failed to start server on port " + port, e);
        }
    }

    /**
     * Accepts a new connection from another council member.
     * @return The accepted socket connection.
     * @throws VotingException if acceptance fails.
     */
    public Socket acceptConnection() throws VotingException {
        try {
            Socket socket = serverSocket.accept();
            LOGGER.info("Accepted connection from " + socket.getRemoteSocketAddress());
            return socket;
        } catch (IOException e) {
            throw new VotingException("Failed to accept connection", e);
        }
    }

    /**
     * Connects to another council member.
     * @param host The host address of the member.
     * @param port The port number of the member.
     * @return The connected socket.
     * @throws VotingException if connection fails.
     */
    public Socket connect(String host, int port) throws VotingException {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            LOGGER.info("Connected to " + host + ":" + port);
            return socket;
        } catch (IOException e) {
            throw new VotingException("Failed to connect to " + host + ":" + port, e);
        }
    }

    /**
     * Shuts down the server socket and stops accepting new connections.
     */
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            LOGGER.info("Network manager shut down");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during shutdown", e);
        }
    }

    public boolean isRunning() {
        return running;
    }
}