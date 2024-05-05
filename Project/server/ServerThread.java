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
  private GameBoard gameBoard = new GameBoard();
  private boolean isAway = false;
  private boolean isReady = false;
  private boolean isTurn = false;
  private boolean inGame = false;
  private boolean isSpectator = false;
  

  private void info(String message) { System.out.println(String.format("Thread[%s = \"%s\"]: %s", this.threadId(), this.clientName, message)); }

  public ServerThread(Socket myClient, Room room) {
    info("Thread created");
    this.client = myClient;
    this.currentRoom = room;
  }

  protected synchronized void setClientName(String name) {
    Payload payload = new Payload();
    payload.setPayloadType(PayloadType.MESSAGE);
    payload.setRename(true);
    payload.setClientName("Server");
    if (name == null || name.isBlank() || currentRoom.isTakenName(name) || name.equalsIgnoreCase("Server") || name.equalsIgnoreCase("null") || name.equalsIgnoreCase("Lobby")) {
      System.err.println("Invalid client name being set");
      payload.setMessage(name + " is an invalid name. Auto-generating name.");
      name = createClientName();
    } else {
      payload.setMessage("Your name is: " + name);
    }
    this.clientName = name;
    this.gameBoard.setClientName(name);
    send(payload);
  }

  protected synchronized String getClientName() { return clientName; }

  protected synchronized Room getCurrentRoom() {  return currentRoom;  }

  protected synchronized void setCurrentRoom(Room room) {
    if (room != null) currentRoom = room;
    else info("Passed in room was null, this shouldn't happen");
  }

  protected synchronized void setGameBoard(GameBoard board) { 
    // gameBoard = board; // This is a shallow copy -- is this okay?
    this.gameBoard.setBoard(board.getCopy());
    this.gameBoard.setClientName(clientName);
  }

  protected synchronized GameBoard getGameBoard() { return this.gameBoard; }

  protected synchronized boolean inGame() { return inGame; }

  protected synchronized void inGame(boolean inGame) { this.inGame = inGame; }

  protected synchronized boolean isAway() { return isAway; }

  protected synchronized void isAway(boolean isAway) { this.isAway = isAway; }

  protected synchronized boolean isReady() { return isReady; }

  protected synchronized void isReady(boolean isReady) { this.isReady = isReady; }

  protected synchronized boolean isTurn() { return isTurn; }

  protected synchronized void isTurn(boolean isTurn) { this.isTurn = isTurn; }

  protected synchronized boolean isSpectator() { return isSpectator; }

  protected synchronized void isSpectator(boolean isSpectator) { this.isSpectator = isSpectator; }

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

  public synchronized void sendGameEvent(Payload p) {
    info("Sending game event: \n" + p + "\nto room: " + currentRoom.getName());
    send(p);
  }

  public boolean sendPing() {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.PING);
    return send(p);
  }

  protected String createClientName() {
    String name = currentRoom.getUniqueName();
    info("Auto-generated name for client: " + name);
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
      while (isRunning && (fromClient = (Payload) in.readObject()) != null) processPayload(fromClient);
    } catch (Exception e) {
      e.printStackTrace();
      currentRoom.removeClient(this);
      info("Client disconnected");
    } finally {
      isRunning = false;
      info("Exited thread loop. Cleaning up connection");
      cleanup();
    }
  }

  void processPayload(Payload p) {
    info("Received: " + p);
    switch (p.getPayloadType()) {
      case CONNECT -> setClientName(p.getClientName());
      case DISCONNECT -> disconnect();
      case GAME_START -> {
        if (currentRoom != null)
          for (BattleshipThread game : currentRoom.getGames())
            if (game.threadId() == p.getNumber())  game.addPlayer(this);
      }
      case GAME_PLACE, GAME_TURN -> {
        if (currentRoom != null) currentRoom.pushGamePayload(this, p);
      }
      case MESSAGE -> {
        if (isSpectator) return;
        if (currentRoom != null) currentRoom.sendMessage(this, p.getMessage());
        else  Room.joinRoom("lobby", this);
      }
      case PING -> { /* Do nothing */ }
      default -> throw new IllegalArgumentException("Unexpected value: " + p.getPayloadType()); //? redundant
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