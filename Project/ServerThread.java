import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket client;
    private String clientName;
    private boolean isRunning = false;
    private ObjectOutputStream out;
    private Room currentRoom;

    private void info(String message) { System.out.println(String.format("Thread[%s]: %s", this.clientName, message));
    }

    public ServerThread(Socket myClient, Room room) {
        info("Thread created");
        this.client = myClient;
        this.currentRoom = room;
    }

    public ServerThread(Socket myClient, Room room, String name) {
        info("Thread created");
        this.client = myClient;
        this.currentRoom = room;
        if (name != null && !name.isBlank()) {
            this.clientName = name;
        }
    }

    protected void setClientName(String name) {
        if (name == null || name.isBlank()) {
            System.err.println("Invalid client name being set");
            return;
        }
        clientName = name;
    }

    protected String getClientName() { return clientName; }

    protected synchronized Room getCurrentRoom() { return currentRoom; }

    protected synchronized void setCurrentRoom(Room room) {
        if (room != null) {
            currentRoom = room;
        } else {
            info("Passed in room was null, this shouldn't happen");
        }
    }

    public void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        cleanup();
    }

    public boolean sendMessage(String from, String message) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientName(from);
        p.setMessage(message);
        return send(p);
    }

    public boolean sendConnectionStatus(String who, boolean isConnected) {
        Payload p = new Payload();
        p.setPayloadType(isConnected ? PayloadType.CONNECT : PayloadType.DISCONNECT);
        p.setClientName(who);
        p.setMessage(isConnected ? "connected" : "disconnected");
        return send(p);
    }

    private boolean send(Payload payload) {
        try {
            out.writeObject(payload);
            return true;
        } catch (IOException e) {
            info("Error sending message to client (most likely disconnected)");
            // e.printStackTrace();
            cleanup();
            return false;
        } catch (NullPointerException ne) {
            info("Message was attempted to be sent before outbound stream was opened");
            return true;// true since it's likely pending being opened
        }
    }

    @Override
    public void run() {
        info("Thread starting");
        try (
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream()); 
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            ) {
            this.out = out;
            isRunning = true;
            Payload fromClient;
            while (isRunning && (fromClient = (Payload) in.readObject()) != null) {
                info("Received from client: " + fromClient);
                processMessage(fromClient);
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

    void processMessage(Payload p) {
        switch (p.getPayloadType()) {
            case CONNECT:
                setClientName(p.getClientName());
                break;
            case DISCONNECT:// TBD
                break;
            case MESSAGE:
                if (currentRoom != null) {
                    currentRoom.sendMessage(this, p.getMessage());
                } else {
                    // TODO migrate to lobby
                    Room.joinRoom("lobby", this);
                }
                break;
            default:
                break;

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