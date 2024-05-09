package Project.server;

import Project.common.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Room implements AutoCloseable {
  protected static Server server;
  private final String name;
  private List<ServerThread> clients = new ArrayList<>();
  private List<BattleshipThread> games = new ArrayList<>();
  private boolean isRunning = false;

  private final static String COMMAND_TRIGGER = "/";
  private final static String ROOM = "room";
  private final static String LIST_ROOMS = "rooms";
  private final static String CREATE_ROOM = "createroom";
  private final static String JOIN_ROOM = "joinroom";
  private final static String DISCONNECT = "disconnect";
  private final static String LEAVE_ROOM = "leaveroom";
  private final static String LOGOFF = "logoff";
  private final static String USERS = "users";
  private final static String RENAME = "rename";
  private final static String NAME = "name";
  private final static String PM = "pm";
  private final static String GAME_PLAY = "creategame";
  private final static String GAME_LIST = "games";
  private final static String SPECTATE = "spectate";

  public Room(String name) {
    this.name = name;
    this.isRunning = true;
    this.startHeartbeat();
  }

  private void info(String message) { System.out.println(String.format("Room[%s]: %s", name, message)); }

  public String getName() { return name; }

  protected synchronized void addClient(ServerThread client) {
    if (!isRunning) return;
    client.setCurrentRoom(this);
    if (clients.indexOf(client) > -1) {
      info("Attempting to add a client that already exists");
    } else {
      clients.add(client);
      new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          sendConnectionStatus(client, true);
        }
      }.start();
    }
  }

  protected synchronized void removeClient(ServerThread client) {
    if (!isRunning) return;
    clients.remove(client);
    removeFromGame(client);
    if (!clients.isEmpty()) sendConnectionStatus(client, false);
    checkClients();
  }

  protected synchronized void addGame(BattleshipThread game) { games.add(game); }

  protected synchronized void removeGame(BattleshipThread game) { games.remove(game); }

  protected synchronized void removeFromGame(ServerThread client) {
    List<BattleshipThread> gamesToRemove = new ArrayList<>();
    for (BattleshipThread game : games) {
      if (game.hasPlayer(client)) {
        game.removePlayer(client);
        if (game.getPlayers().isEmpty()) {
          gamesToRemove.add(game);
          game.cleanup();
        }
        info("Removed client: " + client.getClientName() + " from game: " + game.threadId());
      }
    }
    if (!gamesToRemove.isEmpty()){
      games.removeAll(gamesToRemove);
      info("Removed " + gamesToRemove.size() + " game(s): " + gamesToRemove);
    }
  }

  protected synchronized List<ServerThread> getClients() { return clients; }

  protected synchronized List<BattleshipThread> getGames() { return games; }

  protected String getUniqueName() { return server.generateUniqueName(); }

  protected boolean isTakenName(String name) { return server.isNameTaken(name); }

  private void checkClients() { if (!name.equalsIgnoreCase("lobby") && clients.isEmpty()) close(); }

  private boolean processCommands(String message, ServerThread client) {
    boolean wasCommand = false;
    try {
      if (message.startsWith(COMMAND_TRIGGER)) {
        String[] comm = message.split(COMMAND_TRIGGER);
        String part1 = comm[1];
        String[] comm2 = part1.split(" +");
        String command = comm2[0].toLowerCase();
        String roomName;
        wasCommand = true;
        switch (command) {
          case CREATE_ROOM:
            roomName = comm2[1];
            Room.createRoom(roomName, client);
            break;
          case JOIN_ROOM:
            roomName = comm2[1];
            Room.joinRoom(roomName, client);
            break;
          case LIST_ROOMS:
            if (server.listRoomNames().length == 1) {
              client.sendMessage("Server", "No rooms have been created yet");
              break;
            }
            StringBuilder rooms = new StringBuilder();
            rooms.append("Rooms:");
            for (String room : server.listRoomNames())
              if (!room.equalsIgnoreCase("lobby")) rooms.append("\n  - ").append(room).append(" (").append(server.getRoom(room).getClients().size()).append(")");
            client.sendMessage("Server", rooms.toString());
            break;
          case ROOM:
            client.sendMessage("Server", "You are in room: " + this.name);
            break;
          case USERS:
            StringBuilder list = new StringBuilder();
            list.append(clients.size()).append(" Users in room (").append(this.name).append("): \n");
            for (ServerThread c : clients) {
              list.append(c.getClientName());
              if (clients.indexOf(c) < clients.size() - 1) list.append("\n");
            }
            client.sendMessage("Server",list.toString());
            break;
          case NAME:
            if (comm2.length == 1) client.sendMessage("Server", "your name is: " + client.getClientName());
          case RENAME:
            String newName = comm2[1];
            if (!isTakenName(newName)) {
              client.setClientName(newName);
            } else {
              client.sendMessage("Server", "Name already taken");
            }
            break;
          case PM:
            if(client.isSpectator()) {
              client.sendMessage("Server", "You can't send private messages as a spectator");
              break;
            }
            List<ServerThread> targets = new ArrayList<>();
            List<String> targetNames = new ArrayList<>();
            StringBuilder privMsg = new StringBuilder();
            for (String messagePart : comm2) {
              if (messagePart.startsWith("@")) {
                messagePart = messagePart.substring(1);
                for (ServerThread c : clients) {
                  if (c.getClientName().equals(messagePart)) {
                    targets.add(c); 
                    targetNames.add(messagePart);
                  }
                }
              } else {
                if (!messagePart.equalsIgnoreCase(PM)) {
                  if (privMsg.length() > 0) privMsg.append(" ");
                  privMsg.append(messagePart);
                }
              }
            }
            if (privMsg.length() <= 0) {
              client.sendMessage("Server", "No message found");
              break;
            } else if (targets.isEmpty()) {
              client.sendMessage("Server", "No valid targets found");
              break;
            }
            if (targets.isEmpty()) {
              client.sendMessage("Server", "No valid targets found");
            } else {
              for (ServerThread target : targets) {
                StringBuilder finalMsg = new StringBuilder();
                finalMsg.append("Private message with: ");
                finalMsg.append(client.getClientName());
                for (String targetName : targetNames) if (!targetName.equals(target.getClientName())) finalMsg.append(", ").append(targetName);
                finalMsg.append("\n\u001B[32m").append(client.getClientName()).append(": ").append(privMsg.toString());
                target.sendMessage("Server", finalMsg.toString());
              }
              client.sendMessage("Server", "Message sent to " + targets.size() + String.format(" user(s): %s", targetNames));
            }
            break;
          case GAME_LIST:
            if (games.isEmpty()) {
              client.sendMessage("Server", "No games are currently running");
              break;
            }
            StringBuilder gameList = new StringBuilder();
            gameList.append("Games:");
            for (BattleshipThread game : games) {
              gameList.append("\n GameID [").append(game.threadId()).append("] : ").append(game.getPlayers().size()).append(" players : ");
              for(ServerThread player : game.getPlayers()) gameList.append("\n  - ").append(player.getClientName());
            }
            client.sendMessage("Server", gameList.toString());
            break;
          case GAME_PLAY:
            if (name.equalsIgnoreCase("lobby")) {
              client.sendMessage("Server", "You can't start a game in the lobby, create a private room first");
              break;
            }
            info("Starting game.");
            boolean hardDifficulty = false;
            boolean salvoGameMode = false;
            int playerCount = 1;
            StringBuilder gameStartMsg = new StringBuilder();
            List<String> targetUsers = new ArrayList<>();
            for (String messagePart : comm2) {
              if (messagePart.startsWith("@")) {
                targetUsers.add(messagePart.substring(1));
                playerCount++;
              } else if (messagePart.equalsIgnoreCase("hard")) {
                hardDifficulty = true;
              } else if (messagePart.equalsIgnoreCase("salvo")) {
                salvoGameMode = true;
              }
            }
            if (!targetUsers.isEmpty()) playerCount = targetUsers.size() + 1;
            else playerCount = clients.size();
            BattleshipThread newGame = new BattleshipThread(this, hardDifficulty, salvoGameMode, playerCount > 4 ? 4 : playerCount);
            newGame.addPlayer(client);
            gameStartMsg.append(client.getClientName()).append(" is inviting you to play a game of battleship (GameID [").append(newGame.threadId()).append("])\n");
            gameStartMsg.append("Game mode: ").append(salvoGameMode ? "Salvo" : "Classic").append(" | ").append(hardDifficulty ? "Hard" : "Easy").append("\n");
            gameStartMsg.append("To join, type /joingame ").append(newGame.threadId());
            if (targetUsers.isEmpty())
              for(ServerThread clnt : clients){
                if (clnt.getClientName().equals(client.getClientName())) continue;
                clnt.sendMessage("Server", gameStartMsg.toString());
              }
            for (String targetUser : targetUsers)
              for (ServerThread clnt : clients){
                if (clnt.getClientName().equals(targetUser)){
                  clnt.sendMessage("Server", gameStartMsg.toString());
                  newGame.addPlayer(clnt);
                }
              }
            newGame.start();
            games.add(newGame);
            break;
          // case "joingame":
          //   int gameId = Integer.parseInt(comm2[1]);
          //   for (BattleshipThread game : games) {
          //     if (game.threadId() == gameId) {
          //       game.addPlayer(client);
          //       break;
          //     }
          //   }
          //   break;
          case SPECTATE:
            int gameId = Integer.parseInt(comm2[1]);
            for (BattleshipThread game : games) {
              if (game.threadId() == gameId) {
                if (game.hasPlayer(client)) game.removePlayer(client);
                game.addSpectator(client);
                client.isSpectator(true);
                break;
              }
            }
            break;
          case "turn", "board", "boards", "players", "spectators", "game", "leavegame":
            for (BattleshipThread game : games)
              if (game.hasPlayer(client)) {
                game.processCommand(client, message.substring(1));
                return true;
              }
            client.sendMessage("Server", "Invalid command or you are not in a game");
            break;
          case LEAVE_ROOM:
            Room.joinRoom("lobby", client);
            break;
          case DISCONNECT, LOGOFF:
            Room.disconnectClient(client, this);
            break;
          default:
            client.sendMessage("Server", "Invalid command (sending as a message instead)");
            wasCommand = false;
            break;
          }
        }
      } catch (Exception e) {
    }
    return wasCommand;
  }

  protected static void createRoom(String roomName, ServerThread client) {
    if (server.createNewRoom(roomName)) {
      Room.joinRoom(roomName, client);
    } else {
      client.sendMessage("Server", String.format("Room %s already exists", roomName));
    }
  }

  protected static void joinRoom(String roomName, ServerThread client) {
    // if (!server.joinRoom(roomName, client)) client.sendMessage("Server", String.format("Room %s doesn't exist", roomName)); // The message is redundant (server already sends one)
    server.joinRoom(roomName, client);
  }

  protected static void disconnectClient(ServerThread client, Room room) {
    client.setCurrentRoom(null);
    client.disconnect();
    room.removeClient(client);
  }

  protected synchronized void sendMessage(ServerThread sender, String message) {
    if (!isRunning) return;
    info("Sending message to " + (clients.size() - 1) + " clients");
    if (sender != null && processCommands(message, sender)) return;
    String from = (sender == null ? "Room" : sender.getClientName());
    Iterator<ServerThread> iter = clients.iterator();
    while (iter.hasNext()) {
      ServerThread client = iter.next();
      if (client == sender) continue;
      boolean messageSent = client.sendMessage(from, message);
      if (!messageSent) handleDisconnect(iter, client);
    }
  }

  protected synchronized void sendGameEvent(ServerThread player, Payload payload){
    if (!isRunning) return;
    info("Sending game event to " + clients.size() + " clients");
      for (BattleshipThread game : games) {
          if (game == null) continue;
          if (game.hasPlayer(player)) player.sendGameEvent(payload);
      }
  }

  protected synchronized void pushGamePayload(ServerThread player, Payload payload) {
    if (!isRunning) return;
    info("Sending game event to BattleshipThread");
    for (BattleshipThread game : games) {
      if (game == null) continue;
      if (game.hasPlayer(player)) game.processPayload(player, payload);
    }
  }

  protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected) {
    if (sender.getClientName() == null) return;
    Iterator<ServerThread> iter = clients.iterator();
    while (iter.hasNext()) {
      ServerThread client = iter.next();
      if (sender.getClientName().equals(client.getClientName())) continue;
      boolean messageSent = client.sendConnectionStatus(sender.getClientName(), isConnected);
      if (!messageSent) handleDisconnect(iter, client);
    }
  }

  private void startHeartbeat() {
    new Thread(() -> {
      while (true) {
        try {
          Thread.sleep(5000); // wait for 5 seconds
        } catch (InterruptedException e) {
        }
        try{ 
          checkClients();
          Iterator<ServerThread> iter = clients.iterator();
          while (iter.hasNext()) {
            ServerThread client = iter.next();
            if (!client.sendPing()) handleDisconnect(iter, client);
          }
        } catch(Exception e) {}
      }
    }).start();
  }

  private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
    removeFromGame(client);
    iter.remove();
    info("Removed client " + client.getClientName());
    checkClients();
    sendMessage(null, client.getClientName() + " disconnected");
  }

  @Override
  public void close() {
    server.removeRoom(this);
    // server = null; // Was making server null for all rooms
    isRunning = false;
    clients = null;
  }
}
