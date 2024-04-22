import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
  int port = 3001;
  private List<Room> rooms = new ArrayList<Room>();
  private Room lobby = null;

  private void start(int port) {
    this.port = port;
    try (ServerSocket serverSocket = new ServerSocket(port);) {
      Socket incoming_client = null;
      System.out.println("Server is listening on port " + port);
      Room.server = this;
      lobby = new Room("Lobby");
      rooms.add(lobby);
      do {
        System.out.println("waiting for next client");
        if (incoming_client != null) {
          System.out.println("Client connected");
          ServerThread sClient = new ServerThread(incoming_client, lobby);
          sClient.start();
          joinRoom("lobby", sClient);
          incoming_client = null;
        }
      } while ((incoming_client = serverSocket.accept()) != null);
    } catch (IOException e) {
      System.err.println("Error accepting connection");
      e.printStackTrace();
    } finally {
      System.out.println("closing server socket");
    }
  }

  private Room getRoom(String roomName) {
    for (int i = 0, l = rooms.size(); i < l; i++)
      if (rooms.get(i).getName().equalsIgnoreCase(roomName))
        return rooms.get(i);
    return null;
  }

  protected synchronized boolean joinRoom(String roomName, ServerThread client) {
    Room newRoom = roomName.equalsIgnoreCase("lobby") ? lobby : getRoom(roomName);
    Room oldRoom = client.getCurrentRoom();
    if (newRoom != null) {
      if (oldRoom != null) {
        System.out.println(client.getName() + " leaving room " + oldRoom.getName());
        oldRoom.removeClient(client);
      }
      System.out.println(client.getName() + " joining room " + newRoom.getName());
      newRoom.addClient(client);
      return true;
    } else {
      client.sendMessage("Server",
          String.format("Room %s wasn't found, please try another", roomName));
    }
    return false;
  }

  protected synchronized boolean createNewRoom(String roomName) {
    if (getRoom(roomName) != null) {
      // TODO can't create room
      System.out.println(String.format("Room %s already exists", roomName));
      return false;
    } else {
      Room room = new Room(roomName);
      rooms.add(room);
      System.out.println("Created new room: " + roomName);
      return true;
    }
  }

  protected synchronized void removeRoom(Room r) { 
    if (rooms.removeIf(room -> room == r)) System.out.println("Removed empty room " + r.getName());
  }

  protected String generateUniqueName() {
    String baseName = "User";
    int counter = 0;
    String potentialName;
    do {
      counter++;
      potentialName = baseName + counter;
    } while (isNameTaken(potentialName));
    return potentialName;
  }

  protected boolean isNameTaken(String name) {
    for (Room room : rooms)
      for (ServerThread client : room.getClients())
        if (client.getClientName() != null && client.getClientName().equals(name))
          return true;
    return false;
  }

  protected synchronized void broadcast(String message) {
    if (processCommand(message)) return;
    Iterator<Room> it = rooms.iterator();
    while (it.hasNext()) {
      Room room = it.next();
      if (room != null) room.sendMessage(null, message);
    }
  }

  private boolean processCommand(String message) {
    System.out.println("Checking command: " + message);
    // TODO
    return false;
  }

  public static void main(String[] args) {
    System.out.println("Starting Server");
    Server server = new Server();
    int port = 3000;
    try {
      port = Integer.parseInt(args[0]);
    } catch (Exception e) {

    }
    server.start(port);
    System.out.println("Server Stopped");
  }
}