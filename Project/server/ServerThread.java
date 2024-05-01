package Project.server;

import Project.common.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread {
  private final Socket client;
  private String clientName;
  private boolean isRunning = false;
  private ObjectOutputStream out;
  private Room currentRoom;
  private GameBoard gameBoard;
  private boolean isAway = false;
  private boolean isReady = false;
  

  private void info(String message) { System.out.println(String.format("Thread[%s = \"%s\"]: %s", this.threadId(), this.clientName, message)); }

  public ServerThread(Socket myClient, Room room) {
    info("Thread created");
    this.client = myClient;
    this.currentRoom = room;
  }

  protected void setClientName(String name) {
    if (name == null || name.isBlank() || currentRoom.isTakenName(name) || name.equalsIgnoreCase("Server") || name.equalsIgnoreCase("null") || name.equalsIgnoreCase("Lobby")) {
      System.err.println("Invalid client name being set");
      sendMessage("Server", "Invalid name chosen, auto-generating name");
      name = createClientName();
    }
    this.clientName = name;
  }

  protected String getClientName() { return clientName; }

  protected synchronized Room getCurrentRoom() {  return currentRoom;  }

  protected synchronized void setCurrentRoom(Room room) {
    if (room != null) currentRoom = room;
    else info("Passed in room was null, this shouldn't happen");
  }

  protected synchronized void setGameBoard(GameBoard board) { gameBoard = board; }

  protected GameBoard getGameBoard() { return gameBoard; }

  protected boolean isAway() { return isAway; }

  protected void setAway(boolean away) { isAway = away; }

  protected boolean isReady() { return isReady; }

  protected void setReady(boolean ready) { isReady = ready; }

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

  public void sendGameEvent(Payload p) {
    send(p);
  }

  public boolean sendPing() {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.PING);
    return send(p);
  }

  protected String createClientName() {
    String name = currentRoom.getUniqueName();
    System.out.println("Auto-generated name for client: " + name);
    return name;
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
      cleanup();
      return false;
    } catch (NullPointerException ne) {
      info("Message was attempted to be sent before outbound stream was opened");
      return true;
    }
  }

  @Override
  public void run() {
    info("Thread starting");
    try (
        ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
      this.out = out;
      isRunning = true;
      Payload fromClient;
      while (isRunning && (fromClient = (Payload) in.readObject()) != null) {
        info("Received: " + fromClient);
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
      case CONNECT -> setClientName(p.getClientName());
      case DISCONNECT -> disconnect();
      case GAME_PLACE, GAME_TURN -> {
        if (currentRoom != null) currentRoom.sendGameEvent(this, p);
      }
      case MESSAGE -> {
        if (currentRoom != null) currentRoom.sendMessage(this, p.getMessage());
        else  Room.joinRoom("lobby", this);
      }
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