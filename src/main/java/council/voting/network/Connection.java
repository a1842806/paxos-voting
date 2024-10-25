package council.voting.network;

import council.voting.message.Message;
import council.voting.exception.VotingException;

import java.io.*;
import java.net.Socket;

/**
 * Represents a connection to another council member.
 * Manages the socket, input stream, and output stream.
 */
public class Connection {
    private final Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;

    public Connection(Socket socket) throws VotingException {
        this.socket = socket;
        try {
            // Important: OutputStream must be created before InputStream to prevent deadlocks
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.outputStream.flush(); // Flush header
            this.inputStream = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new VotingException("Failed to create streams for socket", e);
        }
    }

    public void sendMessage(Message message) throws VotingException {
        try {
            synchronized (outputStream) {
                outputStream.writeObject(message);
                outputStream.flush();
            }
        } catch (IOException e) {
            throw new VotingException("Failed to send message", e);
        }
    }

    public Message receiveMessage() throws VotingException {
        try {
            synchronized (inputStream) {
                return (Message) inputStream.readObject();
            }
        } catch (IOException e) {
            throw new VotingException("Failed to receive message", e);
        } catch (ClassNotFoundException e) {
            throw new VotingException("Received unknown object type", e);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            // Optionally log the exception
            // LOGGER.log(Level.WARNING, "Failed to close socket", e);
        }
    }
}