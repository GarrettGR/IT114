package Module4.Part3HW;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client;
    private String userName;
    private boolean isRunning = false;
    private ObjectOutputStream out;
    private Server server;

    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", this.userName, message));
    }

    public ServerThread(Socket myClient, Server server, String userName) {
        info("Thread created");
        // get communication channels to single client
        this.client = myClient;
        this.server = server;
        this.userName = userName;
    }

    public void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        cleanup();
    }

    public boolean send(String message) {
        try {
            out.writeObject(message);
            return true;
        } catch (IOException e) {
            info("Error sending message to client (most likely disconnected)");
            // e.printStackTrace();
            cleanup();
            return false;
        }
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            this.out = out;
            isRunning = true;
            String fromClient;
            try {
                while (isRunning && (fromClient = (String) in.readObject()) != null) {
                    info("Received from client: " + fromClient);
                    server.broadcast(fromClient, this.userName);
                }
            } catch (EOFException e) {
                info("Client disconnected");
            } catch (IOException e) {
                info("Error reading from client");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            info("Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
        }
    }

    private void cleanup() {
        info("Thread cleanup() start");
        try {
            client.close();
        } catch (IOException e) {
            info("Client already closed");
        }
        info("Thread cleanup() complete");
    }
}