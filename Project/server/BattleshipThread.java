package Project.server;

import Project.common.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors; //? Use a semaphore instead?
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BattleshipThread extends Thread { //? implement auto-closeable?
  private final Room room;
  private boolean hardDifficulty;
  private boolean salvoGameMode;
  private boolean started;
  private boolean isRunning = false;
  private String phase;
  private countDown counterTimer;
  private ServerThread currentPlayer; //? make this volatile?

  private Map<ServerThread, PlayerData> players = new HashMap<>();
  private Iterator<ServerThread> playerIterator;
  private List<ServerThread> spectators = new ArrayList<>();

  // private Map<ShipType, Integer> shipCounts = Map.of(
  //     ShipType.CARRIER, 1,
  //     ShipType.BATTLESHIP, 1,
  //     ShipType.CRUISER, 2,
  //     ShipType.SUBMARINE, 2,
  //     ShipType.DESTROYER, 3,
  //     ShipType.LIFE_BOAT, 1
  //   );

  private Map<ShipType, Integer> shipCounts = Map.of( //! testing with fewer ships to save time
    ShipType.CARRIER, 0,
    ShipType.BATTLESHIP, 0,
    ShipType.CRUISER, 0,
    ShipType.SUBMARINE,  0,
    ShipType.DESTROYER, 1,
    ShipType.LIFE_BOAT, 0
  );

  protected static final String ANSI_RESET = "\u001B[0m";
  protected static final String ANSI_ORANGE = "\u001B[38;2;255;165;0m";
  protected static final String ANSI_YELLOW = "\u001B[33m";
  protected static final String ANSI_RED = "\u001B[31m";
  protected static final String ANSI_GRAY_BG = "\u001B[48;2;35;35;35m";
  protected static final String ANSI_GRAY = "\u001B[38;2;150;150;150m";
  protected static final String UNIX_CLEAR = "\033[H\033[2J";
  private static final String TURN = "turn";
  private static final String BOARD = "board";
  private static final String BOARDS = "boards";
  private static final String PLAYERS = "players";
  private static final String SPECTATORS = "spectators";
  private static final String GAME = "game";
  private static final String LEAVE_GAME = "leave game";


  public BattleshipThread(Room room, boolean hardDifficulty, boolean salvoGameMode, int playerCount) {
    this.hardDifficulty = hardDifficulty;
    this.salvoGameMode = salvoGameMode;
    this.room = room;
    playerIterator = players.keySet().iterator();
    printGameInfo("Battleship game thread created");
    counterTimer = new countDown(() -> { placementPhase(); }, playerCount < 4 ? playerCount : 4, 256);
  }

  protected static void printGameInfo(String message) { System.out.println(ANSI_GRAY_BG + ANSI_YELLOW + message + ANSI_RESET); }

  public void sendGameState(ServerThread player, PayloadType type, String message, String privledgedMessage) {
    Payload payload = new Payload();
    payload.setPayloadType(type);
    payload.setClientName("Game"); //? should this be the client/player name?
    payload.setRoomName(room.getName());
    payload.setMessage(message);
    payload.setNumber((long) (players.size() - 1));
    payload.setPlayerData(getPlayerMap(players));
    for (ServerThread p : players.keySet()) { 
      if (p == null) continue;
      if (p == currentPlayer) payload.setTurn(true);
      if (p == player) continue;
      payload.setPlayerBoard(p.getGameBoard());
      payload.setOpponentBoards(getOpponentBoards(p));
      p.sendGameEvent(payload);
    }
    payload.setMessage(privledgedMessage);
    payload.setOpponentBoards(getOpponentBoards(player));
    payload.setPlayerBoard(player.getGameBoard());
    player.sendGameEvent(payload);
    for (ServerThread p : players.keySet()) if (p != null) if (player != p) payload.addOpponentBoard(p.getClientName(), p.getGameBoard()); // let spectators see all board information
    for (ServerThread spec : spectators) if (spec != null) spec.sendGameEvent(payload);
  }

  public void sendGameMessage(ServerThread player, String message) {
    Payload payload = new Payload();
    payload.setPayloadType(PayloadType.MESSAGE);
    payload.setClientName("Game");
    payload.setMessage(message);
    player.sendGameEvent(payload);
  }

  public void sendGameMessage(String message) {
    Payload payload = new Payload();
    payload.setPayloadType(PayloadType.MESSAGE);
    payload.setClientName("Game");
    payload.setMessage(message);
    for (ServerThread player : players.keySet()) player.sendGameEvent(payload);
    for (ServerThread spectator : spectators) spectator.sendGameEvent(payload);
  }

  // private void sendGameEnd () {}

  public void processCommand(ServerThread player, String command) {
    String[] parts = command.split(" ");
    switch(parts[0].toLowerCase()) {
      case TURN -> {
        if (phase.equals("placement"))  sendGameMessage(player, "The game is in the placement phase, it has not started yet");
        else if (currentPlayer == player) sendGameMessage(player, "It is your turn");
        else sendGameMessage(player, String.format("It is %s's turn", currentPlayer.getClientName()));
      }
      case BOARD -> {
        if (player.getGameBoard() == null) sendGameMessage(player, "You have not placed your ships yet");
        else sendGameMessage(player, player.getGameBoard().toString());
      }
      case BOARDS -> {
        StringBuilder sb = new StringBuilder();
        sb.append("Boards: \n");
        for (ServerThread p : players.keySet()) {
          if (p == null) continue;
          sb.append(p.getGameBoard().getProtectedCopy().toString()).append("  ");
          for (int i = 0; i < 30; i++) sb.append("-");
          sb.append("\n");
        }
        sendGameMessage(player, sb.toString());
      }
      case PLAYERS -> {
        StringBuilder sb = new StringBuilder();
        sb.append("Players: ");
        for (ServerThread p : players.keySet()) {
          if (p == null) continue;
          sb.append("\n  - ").append(p.getClientName());
        }
        sendGameMessage(player, sb.toString());
      }
      case SPECTATORS -> {
        StringBuilder sb = new StringBuilder();
        sb.append("Spectators: ");
        for (ServerThread spec : spectators) {
          if (spec == null) continue;
          sb.append("\n  - ").append(spec.getClientName());
        }
        sendGameMessage(player, sb.toString());
      }
      case GAME -> sendGameMessage(player, String.format("You are in room: %s, playing game: %s, which is currerntly in: %s phase", room.getName(), this.threadId(), phase));
      case LEAVE_GAME -> {
        if (players.keySet().contains(player)) {
          sendGameMessage(player, "You have left the game");
          removePlayer(player);
          sendGameMessage(player.getClientName() + " has left the game");
        } else if (spectators.contains(player)) {
          sendGameMessage(player, "You are no longer spectating the game");
          removeSpectator(player);
          sendGameMessage(player.getClientName() + " has stopped spectating the game");
        } else {
          sendGameMessage(player, "You are not in the game");
        }
      }
      default -> sendGameMessage(player, "Invalid command");
    }
  }

  public void processPayload(ServerThread player, Payload payload) {
    switch (payload.getPayloadType()) {
      case GAME_START ->  addPlayer(player);
      case GAME_PLACE -> {
        if (started || player.getGameBoard().hasShips()) {
          player.sendMessage("Game", "You cannot place any ships right now");
          return;
        }
        printGameInfo("Placing ships");
        List<Ship> ships = payload.getShipList();
        if (!validateShipCounts(ships)) { 
          System.err.println("Invalid ship counts");
          return;
        }
        if (!validateShipPlacements(ships, player.getGameBoard())) {
          System.out.println("Invalid ship placement");
          sendGameMessage(player, "Invalid ship placement");
          return;
        }
        printGameInfo("Ships: ");
        for (Ship ship : ships) printGameInfo("  - " + ship);
        System.out.println();

        GameBoard board = player.getGameBoard() != null ? new GameBoard(player.getGameBoard()) : new GameBoard();
        for (Ship ship : ships) board.placeShip(ship);

        player.setGameBoard(board);
        printGameInfo("Board:");
        printGameInfo(board.toString());

        printGameInfo("Ships placed successfully for " + player.getClientName()); 
        sendGameMessage(player, "You have placed your ships, waiting for other players");
        counterTimer.decrement();
      }
      case GAME_TURN -> {
        printGameInfo("Taking turn");
        if (!started || !player.getGameBoard().hasShips()) return; // make them a spectator -- send a message they can't do that?
        Map<String, List<Integer[]>> targetCoordinates = payload.getCoordinates();
        printGameInfo("Target coordinates: ");
        for (String name : targetCoordinates.keySet()) {
          printGameInfo("  - " + name);
          for (Integer[] coordinate : targetCoordinates.get(name)) printGameInfo("    - " + coordinate[0] + ", " + coordinate[1]);
        }
        for (String name : targetCoordinates.keySet()) {
          List<Integer[]> coordinates = targetCoordinates.get(name);
          if (coordinates.isEmpty()) {
            sendGameMessage(player, "You must target at least one location");
            return;
          }
          if (!salvoGameMode && coordinates.size() > 1) {
            sendGameMessage(player, "You can only target one location in Classic mode");
            return;
          }
          if (getPlayer(name) == null) {
            sendGameMessage(player, "Invalid target");
            return;
          }
          GameBoard targetBoard = getPlayer(name).getGameBoard();
          for (Integer[] coordinate : coordinates) {
            int x = coordinate[0];
            int y = coordinate[1];
            if (x < 0 || x >= targetBoard.getBoardSize() || y < 0 || y >= targetBoard.getBoardSize()) {
              sendGameMessage(player, "Invalid coordinates");
              return;
            } else if (targetBoard.getPiece(x, y) == PieceType.HIT || targetBoard.getPiece(x, y) == PieceType.MISS) {
              sendGameMessage(player, "You have already targeted that location");
              return;
            }
          }
        }
        handleAttack(player, targetCoordinates);
      }
      default -> throw new IllegalArgumentException("Unexpected value: " + payload.getPayloadType());
    }
  }

  public String getPhase() { return phase; }

  protected synchronized void addPlayer(ServerThread player) {
    if (!started) {
      int health = 0;
      for (ShipType shipType : shipCounts.keySet()) for (int i=0; i<shipCounts.get(shipType); i++) health += shipType.getLength();
      PlayerData tempPlayer = new PlayerData(health);
      players.put(player, tempPlayer);
      counterTimer.decrement();
    }
  }

  protected synchronized void addSpectator(ServerThread spectator) { spectators.add(spectator); }

  protected synchronized void removePlayer(ServerThread player) { 
    players.remove(player); 
  }

  protected synchronized void removeSpectator(ServerThread spectator) { spectators.remove(spectator); }

  protected boolean hasPlayer(ServerThread player) { return players.keySet().contains(player);}

  protected boolean hasPlayer(String name) {
    for (ServerThread player : players.keySet()) if (player.getClientName().equals(name)) return true;
    return false;
  }

  protected boolean hasSpectator(ServerThread spectator) { return spectators.contains(spectator); }

  protected boolean hasSpectator(String name) {
    for (ServerThread spectator : spectators) if (spectator.getClientName().equals(name)) return true;
    return false;
  }

  protected List<ServerThread> getPlayers() { 
    List<ServerThread> plyrs = new ArrayList<>();
    for (ServerThread player : players.keySet()) plyrs.add(player);
    return plyrs; 
  }

  protected ServerThread getPlayer(String name) {
    for (ServerThread player : players.keySet()) if (player.getClientName().equals(name)) return player;
    return null;
  }

  private PlayerData getPlayer(ServerThread player) { return players.get(player); }


  private Map<String, PlayerData> getPlayerMap(Map<ServerThread, PlayerData> p) {
    Map<String, PlayerData> players = new HashMap<>();
    for (ServerThread player : p.keySet()) players.put(player.getClientName(), p.get(player));
    return players;
  }

  private Map<String, GameBoard> getOpponentBoards(ServerThread player) {
    Map<String, GameBoard> boards = new HashMap<>();
    for (ServerThread opponent : players.keySet()) {
      if (opponent == player) continue;
      GameBoard board = opponent.getGameBoard().getProtectedCopy();
      board.setClientName(opponent.getClientName() + "'s");
      boards.put(opponent.getClientName(), board);
    }
    return boards;
  }

  private boolean validateShipCounts(List<Ship> ships) { 
    Map<ShipType, Integer> tempShipCounts = new HashMap<>(shipCounts);
    for (Ship ship : ships) {
      if (tempShipCounts.get(ship.getType()) == 0) return false;
      tempShipCounts.put(ship.getType(), tempShipCounts.get(ship.getType()) - 1);
    }
    return true;
  }

  private boolean validateShipPlacements(List<Ship> ships, GameBoard gameBoard) {
    if (gameBoard == null) return false;
    GameBoard tempGameBoard = new GameBoard(gameBoard);
    for (Ship ship : ships) if (!tempGameBoard.placeShip(ship)) return false;
    return true;
  }

  private ServerThread getNextPlayer() {
    if (!playerIterator.hasNext()) playerIterator = players.keySet().iterator();
      try {
        ServerThread player = playerIterator.next();
      if (!player.getGameBoard().hasShips()) {
        sendGameMessage(player, String.format("%s You lost all your ships, but you can still watch as a spectator", UNIX_CLEAR));
        removePlayer(player);
        addSpectator(player);
        return getNextPlayer();
      }
      return player;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private void placementPhase() {
    if(players.size() < 2 || players.size() > 4) return;
    printGameInfo("Begin placmemnt phase");
    phase = "placement";
    for (ServerThread player : players.keySet()) {      
      Payload p = new Payload();
      p.setPayloadType(PayloadType.GAME_PLACE);
      p.setClientName("Game");
      p.setMessage("Place your ships, you have 3 minutes");
      p.setPlayerBoard(player.getGameBoard());
      for (ShipType type : shipCounts.keySet()) {
        for (int i = 0; i < shipCounts.get(type); i++) {
          Ship ship = new Ship(type);
          p.addShip(ship);
        }
      }
      p.setNumber((long) players.size());
      player.sendGameEvent(p);
    }
  }

  private void gamePhaseInitializer() {
    printGameInfo("Begin game phase");
    phase = "game";
    started = true;
    for (ServerThread player : players.keySet()) sendGameMessage(player, "The game has started"); //? unnecessary?

    // printGameInfo("The player order is: "); //? how can I randomize the order of a hashmap?
    // for (ServerThread player : players.keySet()) printGameInfo("  - " + player.getClientName());

    playerIterator = players.keySet().iterator(); //? could I have it getNextPlayer() a 'random' number of times?
    currentPlayer = getNextPlayer();

    printGameInfo("The current player is: " + currentPlayer.getClientName());

    sendGameState(currentPlayer, PayloadType.GAME_STATE, String.format("Its %s's turn.", currentPlayer.getClientName()), "It is your turn");
  }

  private synchronized void handleAttack(ServerThread player, Map<String, List<Integer[]>> targetCoordinates) {
    if (player != currentPlayer) return;
    PlayerData attackingPlayer = getPlayer(player);
    StringBuilder regularMessageBuilder = new StringBuilder();
    StringBuilder privledgedMessageBuilder = new StringBuilder();
    printGameInfo(player.getClientName() + " is executing their attack (validated)");

    regularMessageBuilder.append(player.getClientName());
    privledgedMessageBuilder.append("You:");

    printGameInfo(String.format("Player Board for %s:\n%s", player.getClientName(), player.getGameBoard().toString()));
    printGameInfo(String.format("Initial statistics for %s: %s", player.getClientName(), attackingPlayer.toString()));

    for (String name : targetCoordinates.keySet()) {
      List<Integer[]> coordinates = targetCoordinates.get(name);
      GameBoard targetBoard = getPlayer(name).getGameBoard();
      PlayerData targetPlayer = getPlayer(getPlayer(name));
      for (Integer[] coordinate : coordinates) {
        int x = coordinate[0];
        int y = coordinate[1];
        printGameInfo("Targeting " + name + " at " + x + ", " + y);
        if (targetBoard.getPiece(x, y) == PieceType.SHIP) {
          printGameInfo(String.format("%sHit%s", ANSI_RED, ANSI_RESET));

          privledgedMessageBuilder.append(String.format("\n  - %sHit%s one of %s's ships on %s, %s", ANSI_RED, ANSI_ORANGE, name, y+1, x+1));
          regularMessageBuilder.append(String.format("\n  - %sHit%s one of %s's ships", ANSI_RED, ANSI_ORANGE, name));

          targetBoard.setPiece(x, y, PieceType.HIT);
          targetPlayer.decrementHealth();
          attackingPlayer.incrementHits();
          attackingPlayer.incrementScore();
        } else {
          printGameInfo(String.format("%sMiss%s", ANSI_RED, ANSI_RESET));

          privledgedMessageBuilder.append(String.format("\n  - %sMiss%s while targeting %s on %s, %s", ANSI_YELLOW, ANSI_ORANGE, name, y+1, x+1));// TODO: DOUBLE CHECK NOT INVERTED
          regularMessageBuilder.append(String.format("\n  - %sMiss%s while targeting %s", ANSI_YELLOW, ANSI_ORANGE, name));

          targetBoard.setPiece(x, y, PieceType.MISS);
          attackingPlayer.incrementMisses();
        }
        printGameInfo(String.format("Target Board for %s:\n%s", name, targetBoard.toString()));
        printGameInfo(String.format("Statistics for %s: %s", name, targetPlayer.toString()));
      }
      counterTimer.decrement();
    }
    sendGameState(player, PayloadType.MESSAGE, regularMessageBuilder.toString(), privledgedMessageBuilder.toString());
    printGameInfo(String.format("Final statistics for %s: %s", player.getClientName(), attackingPlayer.toString()));
  }

  @Override
  public void run() {
    isRunning = true;
    printGameInfo("Battleship game thread started");
    if (!hardDifficulty) hardDifficulty = false;
    if (!salvoGameMode) salvoGameMode = false;

    printGameInfo("Waiting for players to join");
    counterTimer.runLambdas(); // just a counter that does nothing but wait for up to 4 people to join (or 3 minutes to pass)

    if (players.size() < 2) {
      printGameInfo("Not enough players to start the game");
      sendGameMessage("Not enough players to start the game");
      isRunning = false;
      cleanup();
      return;
    } else if (players.size() > 4) {
      printGameInfo("Too many players to start the game");
      sendGameMessage("Too many players to start the game");
      isRunning = false;
      cleanup();
      return;
    }

    printGameInfo("All players who are going to play have joined");
    printGameInfo("Game thread running");

    counterTimer = new countDown(() -> { gamePhaseInitializer(); }, () -> { 
      gamePhaseInitializer(); 
      for (ServerThread player : players.keySet()) {
        if (!player.getGameBoard().hasShips()) {
          sendGameMessage(player, "You have not placed your ships");
          addSpectator(player);
          removePlayer(player);
        }
        sendGameMessage(player, "Timeout reached, starting game phase, any players who have not placed their ships will be spectators, and any unplaced ships will be lost");
      }
    }, players.size(), 180);

    printGameInfo("Waiting for players to place ships");
    counterTimer.runLambdas(); // waits for all players to place their ships (or 3 minutes to pass)

    printGameInfo("All players who are playing have placed their ships");
    printGameInfo("Game Flow: turns starting");

    while (isRunning) {
      printGameInfo("Waiting for " + currentPlayer.getClientName() + " to take their turn");

      counterTimer = new countDown(() -> { 
        printGameInfo("The old current player was: " + ANSI_RED + currentPlayer.getClientName() + ANSI_YELLOW + " and they have finished their turn");
        currentPlayer = getNextPlayer();
        if (currentPlayer == null) {
          printGameInfo("No players left to take a turn");
          cleanup(); //? use an endGame() method?
        }
        sendGameState(currentPlayer, PayloadType.GAME_STATE, String.format("Its %s's turn.", currentPlayer.getClientName()), "It is your turn");
      }, () -> {
        printGameInfo("The old current player was: " + ANSI_RED + currentPlayer.getClientName() + ANSI_YELLOW+ " and they have run out of time");
        sendGameMessage(currentPlayer, "Unfortunatley, you have run out of time, you will be skipped this turn");
        currentPlayer = getNextPlayer();
        if (currentPlayer == null) {
          printGameInfo("No players left to take a turn");
          cleanup(); //? use an endGame() method?
        }
        sendGameState(currentPlayer, PayloadType.GAME_STATE, String.format("Its %s's turn.", currentPlayer.getClientName()), "It is your turn");
      }, players.size() - 1, 60);
        if (players.size() <= 1) break;
        counterTimer.runLambdas();
    }

    //! Handle game end

    isRunning = false;
  }
  protected void cleanup() {
    counterTimer.close();
    counterTimer = null;
    isRunning = false;
    room.removeGame(this);
    printGameInfo("Game thread has ended");
  }
}

// Yes, i know best practice is for a new file, but nothing else will use it
class countDown { //? implement auto-closeable?
  private final Runnable lambda1;
  private final Runnable lambda2;
  private final int timeoutSeconds;
  private long startTime;
  private long timeRemaining;
  private CountDownLatch latch;
  private ScheduledExecutorService executor;  

  public countDown(Runnable lambda1, Runnable lambda2, int latchCount, int timeoutSeconds) {
    this.lambda1 = lambda1;
    this.lambda2 = lambda2;
    this.timeoutSeconds = timeoutSeconds;
    this.latch = new CountDownLatch(latchCount);
    this.executor = Executors.newScheduledThreadPool(1);
    BattleshipThread.printGameInfo("Countdown created");
  }

  public countDown(Runnable lambda1, int latchCount, int timeoutSeconds) {
    this(lambda1, lambda1, latchCount, timeoutSeconds);
  }

  public void decrement() {
    BattleshipThread.printGameInfo("Countdown decremented");
    latch.countDown();
  }

  public long getTimeRemaining() { return timeRemaining; }

  private void printTimer() {
    if (timeRemaining < 10) System.out.print(BattleshipThread.ANSI_RESET + BattleshipThread.ANSI_RED + "Time remaining: " + timeRemaining + BattleshipThread.ANSI_RESET + "  \r");
    else if (timeRemaining < 30) System.out.print(BattleshipThread.ANSI_RESET + BattleshipThread.ANSI_YELLOW + "Time remaining: " + timeRemaining + BattleshipThread.ANSI_RESET + "  \r");
    else System.out.print(BattleshipThread.ANSI_RESET + BattleshipThread.ANSI_GRAY + "Time remaining: " + timeRemaining + BattleshipThread.ANSI_RESET + "  \r");
  }

  public void runLambdas() {
    if (executor != null) executor.shutdownNow();
    executor = Executors.newScheduledThreadPool(1);

    startTime = System.currentTimeMillis();

    Thread counter = new Thread(() -> {
      try {
        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
          BattleshipThread.printGameInfo("\nTimeout occurred");
          lambda2.run();
        } else {
          BattleshipThread.printGameInfo("Countdown completed");
          lambda1.run();
        }
        executor.shutdownNow();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
  
    counter.start();

    try {
      ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
      long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
      timeRemaining = timeoutSeconds - elapsedTime;
        printTimer();
      }, 0, 1, TimeUnit.SECONDS);
      try {
        counter.join();
        if (latch.getCount() == 0) {
          future.cancel(true);
          executor.shutdown();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } catch (Exception e) {}
  }

  protected void close() {
    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
          executor.shutdownNow();
        } 
      } catch (InterruptedException e) {
          executor.shutdownNow();
      }
    }
  }
}