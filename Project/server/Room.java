package Project.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import Project.common.*;

public class Room implements AutoCloseable {
  protected static Server server;
  private String name;
  private List<ServerThread> clients = new ArrayList<ServerThread>();
  private List<BattleshipThread> games = new ArrayList<BattleshipThread>();
  private boolean isRunning = false;

  private final static String COMMAND_TRIGGER = "/";
  private final static String LIST_ROOMS = "rooms";
  private final static String CREATE_ROOM = "createroom";
  private final static String JOIN_ROOM = "joinroom";
  private final static String DISCONNECT = "disconnect";
  private final static String LOGOUT = "logout";
  private final static String LOGOFF = "logoff";
  private final static String USERS = "users";
  private final static String RENAME = "rename";
  private final static String PM = "pm";
  private final static String GAME_START = "play";
  private final static String GAME_LIST = "games";

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
    if (clients.size() > 0) sendConnectionStatus(client, false);
    checkClients();
  }

  protected List<ServerThread> getClients() { return new ArrayList<>(clients); }

  protected String getUniqueName() { return new String(server.generateUniqueName()); }

  protected boolean isTakenName(String name) { return server.isNameTaken(name); }

  private void checkClients() { if (!name.equalsIgnoreCase("lobby") && clients.size() == 0) close(); }

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
            StringBuilder rooms = new StringBuilder();
            rooms.append("Rooms: \n");
            for (String room : server.listRoomNames()) {
              rooms.append(room);
              if (!room.equalsIgnoreCase("lobby")) rooms.append("\n");
            }
            client.sendMessage("Server", rooms.toString());
            break;
          case USERS:
            StringBuilder list = new StringBuilder();
            list.append(clients.size() + " Users in room (" + this.name + "): \n");
            for (ServerThread c : clients) {
              list.append(c.getClientName());
              if (clients.indexOf(c) < clients.size() - 1) list.append("\n");
            }
            client.sendMessage("Server",list.toString());
            break;
          case RENAME:
            String newName = comm2[1];
            if (!isTakenName(newName)) {
              client.setClientName(newName);
            } else {
              client.sendMessage("Server", "Name already taken");
            }
            break;
          case PM:           
            List<ServerThread> targets = new ArrayList<ServerThread>();
            List<String> targetNames = new ArrayList<String>();
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
            if (targets.size() == 0) {
              client.sendMessage("Server", "No valid targets found");
            } else {
              for (ServerThread target : targets) {
                StringBuilder finalMsg = new StringBuilder();
                finalMsg.append("Private message with: ");
                finalMsg.append(client.getClientName());
                for (String targetName : targetNames) if (!targetName.equals(target.getClientName())) finalMsg.append(", " + targetName);
                finalMsg.append("\n" + client.getClientName() + ": " + privMsg.toString());
                target.sendMessage("Server", finalMsg.toString());
              }
              client.sendMessage("Server", "Message sent to " + targets.size() + String.format(" user(s): %s", targetNames));
            }
            break;
          case GAME_LIST:
            StringBuilder gameList = new StringBuilder();
            gameList.append("Games:");
            for (BattleshipThread game : games) {
              gameList.append("\n GameID [" + game.threadId() + "] : " + game.getPlayers().size() + " players : ");
              for(ServerThread player : game.getPlayers()) gameList.append("\n  - " + player.getClientName());
            }
            client.sendMessage("Server", gameList.toString());
            break;
          case GAME_START:
            info("Starting game.");
            boolean hardDifficulty = false;
            boolean salvoGameMode = false;
            StringBuilder gameStartMsg = new StringBuilder();
            List<String> targetUsers = new ArrayList<String>();
            for (String messagePart : comm2) {
              if (messagePart.startsWith("@")) {
                targetUsers.add(messagePart.substring(1));
              } else if (messagePart.equalsIgnoreCase("hard")) {
                hardDifficulty = true;
              } else if (messagePart.equalsIgnoreCase("salvo")) {
                salvoGameMode = true;
              }
            }
            BattleshipThread newGame = new BattleshipThread(this, hardDifficulty, salvoGameMode);
            newGame.addPlayer(client);
            gameStartMsg.append(client.getClientName() + " is inviting you to play a game of battleship (GameID [" + newGame.threadId() + "])\n");
            gameStartMsg.append("Game mode: " + (salvoGameMode ? "Salvo" : "Classic") + " | " + (hardDifficulty ? "Hard" : "Easy") + "\n");
            gameStartMsg.append("To join, type /joingame " + newGame.threadId());
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
            games.add(newGame);
            break;
          case DISCONNECT:
          case LOGOUT:
          case LOGOFF:
            Room.disconnectClient(client, this);
            break;
          default:
            wasCommand = false;
            break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
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
    if (!server.joinRoom(roomName, client)) client.sendMessage("Server", String.format("Room %s doesn't exist", roomName));
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
    Iterator<BattleshipThread> iter = games.iterator();
    while (iter.hasNext()) {
      BattleshipThread game = iter.next();
      if (game == null) continue;
      if (game.hasPlayer(player)) {
        game.pushPayload(player, payload);
      }
    }
  }

  protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected) {
    if (sender.getClientName() == null) return;
    Iterator<ServerThread> iter = clients.iterator();
    while (iter.hasNext()) {
      ServerThread client = iter.next();
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
          e.printStackTrace();
        }
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
          ServerThread client = iter.next();
          if (!client.sendPing()) handleDisconnect(iter, client);
        }
      }
    }).start();
  }

  private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
    iter.remove();
    info("Removed client " + client.getClientName());
    checkClients();
    sendMessage(null, client.getClientName() + " disconnected");
  }

  public void close() {
    server.removeRoom(this);
    server = null;
    isRunning = false;
    clients = null;
  }
}
