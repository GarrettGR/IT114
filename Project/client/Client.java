package Project.client;

import Project.common.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Client {

  // --- Start of variables ---

  Socket server = null;
  ObjectOutputStream out = null;
  ObjectInputStream in = null;
  GUI gui = null;

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_ORANGE = "\u001B[38;2;255;165;0m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GRAY_BG = "\u001B[48;2;35;35;35m";
  public static final String ANSI_GRAY = "\u001B[38;2;150;150;150m";
  public static final String UNIX_CLEAR = "\033[H\033[2J";
  public static final String SQUARE = "\u25A0";

  final String ipAddressPattern = "/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})";
  final String localhostPattern = "/connect\\s+(localhost:\\d{3,5})";

  private static final String HELP = """
      /connect [ip address]:[port] - Connect to a server
      /connect localhost:[port] - Connect to a server on localhost
      /disconnect - Disconnect from the server
      
      /name - see your current username
      /name [username] - Set or change username
      /rename [username] - Set or change username
      /users - List users in the chat
      
      /pm @[username] ... @[username] [message] - Send a private message
      /createroom [room name] - Create a private room
      /joinroom [room name] - Join a private room
      /leaveroom - Leave a private room (to Lobby)
      /rooms - List all available rooms
      /room - Show the room you are currently in
      
      /creategame - Start a game (open to everyone)
      /creategame @[username] - Start a game with a user
      /games - List all currently active games
      
      /clear - Clear the console
      /quit - Exit the program
      /help - Show this help message""";
  private static final String GAME_HELP = """
      /turn - Show whose turn it is
      /attack @[player] [column] [row] - Attack a position on the board of that player
      /place [ship] [column] [row] [direction] - Place a ship on your board
          note: with one end at the location provided, direction = 'up', 'down', 'left', 'right'

      /multishot @[player] [column] [row] [column] [row] - Attack two positions on the board of that player, costs 10 currency

      /board - Show your board
      /ships - List the ships you have left to place
      /boards - Show all player's boards
      /players - Show all players in the game
      /spectators - Show all spectators in the game

      /game - Show the game you are currently in
      /leavegame - Leave the game
      /spectate - Stop playing and watch the game instead 
      /away - Stay in the game, but skip your turn
      /resume - Resume playing after being away

      /help - Show this help message"""; //! STILL HAVE TO ADD /SPECTATE


  boolean isRunning = false;
  private boolean inGame = false;
  private boolean isTurn = false;
  private Thread inputThread;
  private Thread fromServerThread;
  private String clientName = "";
  private List<Ship> ships = new ArrayList<>();
  private List<Ship> placedShips = new ArrayList<>();
  private GameBoard playerBoard = new GameBoard();
  private Map<String, GameBoard> opponentBoards = new HashMap<>();
  private Map<String, List<Integer[]>> coordinates = new HashMap<>();
  private Map<String, PlayerData> playerdata = new HashMap<>();

  // --- Start of Boilerplate ---

  public Client() { 
    this.gui = new GUI(this);
    system_print("Client Created"); 
  }

  public void start() throws IOException { listenForKeyboard(); }

  public static void main(String[] args) {
    Client client = new Client();
    try {
      client.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getClientName() { return clientName; }

  // --- Start of Checkers ---

  public boolean isConnected() {
    if (server == null) return false;
    return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
  }

  private boolean isConnection(String text) { return text.matches(ipAddressPattern) || text.matches(localhostPattern); }

  // --- Start of Formatters ---

  public void server_print(String message) {
    if (message.startsWith("Server:")) {
      System.out.println(ANSI_GRAY_BG + ANSI_YELLOW + message + ANSI_RESET);
      gui.ingestMessage(message);
    } else if (message.startsWith("Game:") || message.startsWith("Room:")) { 
      System.out.println(ANSI_GRAY_BG + ANSI_ORANGE + message + ANSI_RESET);
      gui.ingestMessage(message);
    } else { 
      System.out.println(ANSI_GRAY_BG + message + ANSI_RESET);
      gui.ingestMessage(message);
    }
  }

  public void system_print(String message) { 
    System.out.println(ANSI_YELLOW + message + ANSI_RESET); 
    gui.ingestMessage(message);
  }

  public void system_error(String message) { 
    System.out.println(ANSI_RED + message + ANSI_RESET); 
    gui.ingestMessage(message);
  }

  public void game_print(String message) { 
    System.out.print(ANSI_GRAY + message + ANSI_RESET);
    gui.ingestMessage(message);
  }

  private void drawGame(GameBoard playerBoard, GameBoard[] opponentBoards, String message) {
    for (GameBoard board : opponentBoards) {
      System.out.print(board.toString() + "  ");
      for (int i = 0; i < 30; i++) System.out.print("-");
      System.out.println();
      PlayerData playerData = playerdata.get(board.getClientName().substring(0, board.getClientName().length() - 2));
      if (playerData != null) System.out.println(String.format("%s statistics: %s", board.getClientName(), playerData.toString()));
    }
    System.out.print(playerBoard.toString());
    System.out.println(String.format("%s statistics: %s", playerBoard.getClientName(), playerdata.get(this.clientName)));
    system_print(message);
  }

  // --- Start of Client Input handling ---

  private boolean processCommand(String text) {
    text = text.toLowerCase().trim();
    String[] parts = text.replaceAll(" +", " ").split(" ");
    switch (parts[0]) {
      case "/connect":
        if (isConnected()) {
          system_error("Already connected to a server");
          gui.updateConnection(true);
          return true;
        } else if (!isConnection(text)) {
          system_error("Invalid connection command");
          return true;
        }
        if (clientName.isBlank()) system_print("You can set your name by using: /name [username]");
        String[] connectionParts = parts[1].split(":");
        gui.updateConnection((connect(connectionParts[0].trim(), Integer.parseInt(connectionParts[1].trim()))));
        return true;
      case "/quit":
        isRunning = false;
        return true;
      case "/name", "/rename":
        if (isConnected()) return false;
        if (parts.length == 1) {
          if (clientName.isBlank()) system_print("You have not set a name, use: /name [username]");
          else system_print("Your name is: " + clientName);
        } else {
          clientName = parts[1];
          gui.updateName();
          system_print("Name set to: " + clientName);
        }
        return true;
      case "/allhelp":
        system_print(inGame ? HELP : GAME_HELP);
      case "/help":
        if (inGame) system_print(GAME_HELP);
        else system_print(HELP);
        system_print("/allhelp - Show all server commands / help messages\n");
        return true;
      case "/clear":
        System.out.print(UNIX_CLEAR);
        System.out.flush();
        system_print("Console cleared");
        return true;
      case "/joingame":
        if (parts.length != 2) {
          system_error("Invalid join game command");
          return true;
        }
        sendGameEvent(PayloadType.GAME_START, parts[1]);
        return true;
      case "/ships", "/place", "/attack", "/multishot":
        return processGameCommands(parts);
    }
    return false;
  }

  private boolean processGameCommands(String[] parts) {
    if (!inGame) return false;
    switch (parts[0]) {
      case "/ships":
      printShips();
      return true;
    case "/place":
      placeShips(parts);
      return true;
    case "/attack":
      if (parts.length != 4) system_error("Invalid attack command");
      else attackPlayer(parts[1].substring(1), Integer.parseInt(parts[3]), Integer.parseInt(parts[2]), true);
      return true;
    case "/multishot":
      if (parts.length != 6) {
        system_error("Invalid multi-shot command");
        return true;
      }
      if(playerdata.get(this.clientName).getCurrency() < 10) {
        system_error("You do not have enough currency to use this bonus action!");
        return true;
      }
      attackPlayer(parts[1].substring(1), Integer.parseInt(parts[3]), Integer.parseInt(parts[2]), false);
      attackPlayer(parts[1].substring(1), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), true);
      return false;
    }
    return false;
  }

  // --- Start of Game Logic ---

  private void printShips() {
    if (ships.isEmpty()) {
      system_print("All ships placed");
    } else {
      system_print("You have ships left to place: ");
      for (Ship ship : ships) {
        system_print("  - " + ship.getType().getName() + " : " + ship.getType().getLength());
      }
    }
  }

  private void placeShips(String[] parts) {
    boolean validType = false;
    boolean hasShip = false;
    if (parts.length != 5) {
      system_error("Invalid ship placement");
      return;
    }
    for (ShipType type : ShipType.values()) if (type.getName().equalsIgnoreCase(parts[1])) validType = true;
    if (!validType) {
      system_error("Unrecognized ship type");
      return;
    }
    for (Ship ship : ships) if (ship.getType().getName().equalsIgnoreCase(parts[1])) hasShip = true;
    if (!hasShip) {
      system_error("You have already placed all of that ship available to you");
      return;
    }
    for (Ship ship : ships) {
      if (ship.getType().getName().equalsIgnoreCase(parts[1])) {
        int x = Integer.parseInt(parts[3]);
        int y = Integer.parseInt(parts[2]);
        ship.setAnchorY(y - 1);
        ship.setAnchorX(x - 1);
        ship.setOrientation(parts[4].toLowerCase());
        if (!playerBoard.placeShip(ship)) {
          system_error("Invalid ship placement");
          return;
        }
        placedShips.add(ship);
        system_print("Placed " + ship.getType().getName() + " at " + y + ", " + x + " facing " + parts[4]);
        ships.remove(ship);
        System.out.println(playerBoard.toString());
        HashMap<String, GameBoard> tempBoard = new HashMap<>();
        tempBoard.put(this.clientName, this.playerBoard);
        HashMap<String, PlayerData> tempData = new HashMap<>();
        tempData.put(this.clientName, playerdata.get(this.clientName));
        gui.ingestData(tempData,tempBoard);
        break;
      }
    }
    if (!ships.isEmpty()) {
      system_print("You have ships left to place: ");
      for (Ship ship : this.ships) {
        system_print("  - " + ship.getType().getName() + " - length: " + ship.getType().getLength());
      }
    } else {
      system_print("All ships placed");
      sendGameEvent(PayloadType.GAME_PLACE, this.placedShips);
    }
  }

  private void attackPlayer(String target, int row, int column, boolean finishedAttack) {
    if (!isTurn) {
      system_error("It is not your turn");
      return;
    }
    if (row < 1 || row > 10 || column < 1 || column > 10) {
      system_error("Invalid coordinates");
      return;
    }
    if (opponentBoards.get(target) == null) {
      system_error("Invalid target");
      return;
    }
    if (opponentBoards.get(target).getPiece(row - 1, column - 1) == PieceType.HIT || opponentBoards.get(target).getPiece(row - 1, column - 1) == PieceType.MISS) {
      system_error("You have already targeted that location");
      return;
    }
    if (this.coordinates.get(target) == null) this.coordinates.put(target, new ArrayList<>());
    this.coordinates.get(target).add(new Integer[]{row - 1, column - 1});
    if (finishedAttack) opponentBoards.remove(target);
    // sendGameEvent(PayloadType.GAME_TURN, target, row - 1, column - 1);
    if (this.opponentBoards.isEmpty()) {
      sendGameEvent(PayloadType.GAME_TURN, this.coordinates);
    }
  }

  private void sendGameEvent(PayloadType type, Object ... data){
    Payload p = new Payload();
    p.setPayloadType(type);
    p.setClientName(this.clientName);
    if (null == type) {
      system_error("Invalid game event");
      return;
    } else switch (type) {
      case GAME_START -> p.setNumber(Long.parseLong( (String) data[0]));
      case GAME_PLACE -> {
        // for (Object datum : data) p.addShip((Ship) datum);
        p.setShips(placedShips);
      }
      case GAME_TURN -> {
        // p.addCoordinate((String) data[0], new Integer[]{(int) data[1], (int) data[2]});
        if (!(data[0] instanceof Map)) {
          system_error("Invalid attack position data");
          return;
        }
        p.setCoordinates((Map<String, List<Integer[]>>) data[0]);
        if (!opponentBoards.isEmpty()) return;
      }
      default -> {
        system_error("Invalid game event");
        return;
      }
    }
    sendPayload(p);
  }

  // --- Start of Message Processing & Communications ---

  private void sendConnect() throws IOException {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.CONNECT);
    p.setClientName(clientName);
    out.writeObject(p);
  }

  private void sendMessage(String message) throws IOException {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.MESSAGE);
    p.setMessage(message);
    p.setClientName(clientName);
    out.writeObject(p);
  }

  private void sendPayload(Payload p) { 
    try {
      out.writeObject(p);
    } catch (IOException e) {
    }
  }

  protected void handleMessage(String line) throws IOException {
    if (!processCommand(line)) {
      if (isConnected()) {
        if (line != null && line.trim().length() > 0) sendMessage(line);
      } else {
        system_error("Not connected to server");
      }
    }
  }

  private void listenForKeyboard() {
    inputThread = new Thread() {
      @Override
      public void run() {
        system_print("Enter /help to see a list of commands");
        try (Scanner si = new Scanner(System.in);) {
          String line = "";
          isRunning = true;
          while (isRunning) {
            try {
              line = si.nextLine();
              handleMessage(line);
            } catch (Exception e) {
              system_error("Connection dropped");
              break;
            }
          }
          system_error("Exited loop");
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          close();
        }
      }
    };
    inputThread.start();
  }

  private void listenForServerMessage() {
    fromServerThread = new Thread() {
      @Override
      public void run() {
        try {
          Payload fromServer;
          while (!server.isClosed() && !server.isInputShutdown() && (fromServer = (Payload) in.readObject()) != null) processPayload(fromServer);
          system_error("Loop exited");
        } catch (Exception e) {
          e.printStackTrace();
          if (!server.isClosed()) {
            system_error("Server closed connection");
          } else {
            system_error("Connection closed");
          }
        } finally {
          close();
          system_print("Stopped listening to server input");
        }
      }
    };
    fromServerThread.start();
  }

  private void gameOver() {
    inGame = false;
    isTurn = false;
    placedShips = new ArrayList<>();
    coordinates = new HashMap<>();
    playerBoard = new GameBoard();
    opponentBoards = new HashMap<>();
  }

  private void processPayload(Payload p) {
    switch (p.getPayloadType()) {
      case CONNECT, DISCONNECT -> system_print(String.format("*%s %s*", p.getClientName(), p.getMessage()));
      case MESSAGE -> {
        if (p.isRename()) { 
          this.clientName = p.getMessage().split(" ")[p.getMessage().split(" ").length - 1];
          gui.updateName();
        }
        server_print(String.format("%s: %s", p.getClientName(), p.getMessage()));
      }
      case GAME_PLACE -> {
        system_print("Game started with " + p.getNumber() + " players");
        inGame = true;
        ships = p.getShipList();
        playerBoard.setBoard(p.getPlayerBoard());
        playerBoard.setClientName("Your");
        this.playerdata = p.getPlayerDataMap();
        System.out.println(p.getMessage() + '\n' + playerBoard.toString());
        gui.ingestMessage(p.getMessage());
        HashMap<String, GameBoard> tempBoard = new HashMap<>();
        HashMap<String, PlayerData> tempData = new HashMap<>();
        tempBoard.put(this.clientName, this.playerBoard);
        tempData.put(this.clientName, p.getPlayerData(this.clientName));
        gui.ingestData(tempData, tempBoard);
        printShips();
      }
      case GAME_STATE -> {
        if (p.isGameOver()) {
          gameOver();
          system_print(p.getMessage());
          return;
        }
        this.isTurn = p.isTurn();
        this.placedShips = new ArrayList<>(); // reset turn-specific data
        this.coordinates = new HashMap<>();
        this.playerBoard = new GameBoard(p.getPlayerBoard());
        this.playerBoard.setClientName("Your");
        this.opponentBoards = p.getOpponentBoardsMap();
        this.playerdata = p.getPlayerDataMap();

        HashMap<String, GameBoard> tempBoard = new HashMap<>();
        HashMap<String, PlayerData> tempData = new HashMap<>();

        tempData.putAll(this.playerdata);
        tempBoard.putAll(this.opponentBoards);
        tempBoard.put(this.clientName, this.playerBoard);

        gui.ingestData(tempData, tempBoard);
        drawGame(this.playerBoard, p.getOpponentBoards(), p.getMessage());
      }
      case PING -> { /* do nothing */ }
      default -> throw new IllegalArgumentException("Unexpected value: " + p.getPayloadType());
    }
  }

  // --- Start of Connection handling ---

  private boolean connect(String address, int port) {
    try {
      server = new Socket(address, port);
      out = new ObjectOutputStream(server.getOutputStream());
      in = new ObjectInputStream(server.getInputStream());
      system_print("Client connected");
      listenForServerMessage();
      sendConnect();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return isConnected();
  }

  private void close() {
    try {
      inputThread.interrupt();
    } catch (Exception e) {
      system_error("Error interrupting input");
      e.printStackTrace();
    }
    try {
      fromServerThread.interrupt();
    } catch (Exception e) {
      system_error("Error interrupting listener");
      e.printStackTrace();
    }
    try {
      system_print("Closing output stream");
      out.close();
    } catch (NullPointerException ne) {
      system_print("Server was never opened so this exception is ok");
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      system_print("Closing input stream");
      in.close();
    } catch (NullPointerException ne) {
      system_print("Server was never opened so this exception is ok");
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      system_print("Closing connection");
      server.close();
      system_print("Closed socket");
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NullPointerException ne) {
      system_print("Server was never opened so this exception is ok");
    }
    inGame = false;
    isTurn = false;
    playerBoard = new GameBoard();
  }
}