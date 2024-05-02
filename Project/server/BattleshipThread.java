package Project.server;

import Project.common.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch; //? Use a semaphore instead?
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BattleshipThread extends Thread {
  private final Room room;
  private boolean hardDifficulty;
  private boolean salvoGameMode;
  private boolean started;
  private boolean isRunning = false;
  private String phase;
  private countDown counterTimer;
  private volatile ServerThread currentPlayer;

  private List<ServerThread> players = new ArrayList<>();
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

  private Map<ShipType, Integer> shipCounts = Map.of( // testing with fewer ships to save time
    ShipType.CARRIER, 0,
    ShipType.BATTLESHIP, 1,
    ShipType.CRUISER, 0,
    ShipType.SUBMARINE, 1,
    ShipType.DESTROYER, 0,
    ShipType.LIFE_BOAT, 0
  );

  protected static final String ANSI_RESET = "\u001B[0m";
  protected static final String ANSI_YELLOW = "\u001B[33m";
  protected static final String ANSI_RED = "\u001B[31m";
  protected static final String ANSI_GRAY_BG = "\u001B[48;2;35;35;35m";
  protected static final String ANSI_GRAY = "\u001B[38;2;150;150;150m";

  public BattleshipThread(Room room, boolean hardDifficulty, boolean salvoGameMode, int playerCount) {
    this.hardDifficulty = hardDifficulty;
    this.salvoGameMode = salvoGameMode;
    this.room = room;
    playerIterator = players.iterator();
    printGameInfo("Battleship game thread created\n");
    counterTimer = new countDown(() -> { placementPhase(); }, playerCount, 180);
  }

  protected static void printGameInfo(String message) { System.out.println(ANSI_GRAY_BG + ANSI_YELLOW + message + ANSI_RESET); }

  public void sendGameState(ServerThread player, PayloadType type, String message, String privledgedMessage) {
    Payload payload = new Payload();
    payload.setPayloadType(type);
    payload.setClientName(player.getClientName());
    payload.setMessage(message);
    payload.setNumber((long) (players.size() - 1));
    for (ServerThread p : players) { 
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

    for (ServerThread p : players) if (p != null) if (player != p) payload.addOpponentBoard(p.getClientName(), p.getGameBoard()); // let spectators see all board information
    for (ServerThread spec : spectators) if (spec != null) spec.sendGameEvent(payload);
  }

  public void sendGameMessage(ServerThread player, String message) {
    Payload payload = new Payload();
    payload.setPayloadType(PayloadType.MESSAGE);
    payload.setClientName("Game");
    payload.setMessage(message);
    player.sendGameEvent(payload);
  }

  // private void sendGameEnd () {}

  public void processPayload(ServerThread player, Payload payload) {
    switch (payload.getPayloadType()) {
      case GAME_START ->  addPlayer(player);
      case GAME_PLACE -> {
        if (started || player.getGameBoard().hasShips()) return;
        printGameInfo("Placing ships");
        List<Ship> ships = payload.getShipList();

        if (!validateShipCounts(ships)) { 
          System.err.println("\nInvalid ship counts");
          sendGameMessage(player, "You cannot place any more ships");
          return;
        }
        if (!validateShipPlacements(ships, player.getGameBoard())) {
          System.out.println("\nInvalid ship placement");
          sendGameMessage(player, "Invalid ship placement");
          return;
        }

        printGameInfo("Ships: ");
        for (Ship ship : ships) printGameInfo("  - " + ship);
        System.out.println();

        GameBoard board = player.getGameBoard() != null ? new GameBoard(player.getGameBoard()) : new GameBoard();
        for (Ship ship : ships) board.placeShip(ship);

        printGameInfo(" Board: ");
        printGameInfo(board.toString());

        player.setGameBoard(board);
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
      players.add(player);
      counterTimer.decrement();
    }
  }

  protected synchronized void addSpectator(ServerThread spectator) { spectators.add(spectator); }

  protected synchronized void removePlayer(ServerThread player) { players.remove(player); }

  protected synchronized void removeSpectator(ServerThread spectator) { spectators.remove(spectator); }

  protected boolean hasPlayer(ServerThread player) { return players.contains(player);}

  protected boolean hasPlayer(String name) {
    for (ServerThread player : players) if (player.getClientName().equals(name)) return true;
    return false;
  }

  protected boolean hasSpectator(ServerThread spectator) { return spectators.contains(spectator); }

  protected boolean hasSpectator(String name) {
    for (ServerThread spectator : spectators) if (spectator.getClientName().equals(name)) return true;
    return false;
  }

  protected List<ServerThread> getPlayers() { return players; }

  protected ServerThread getPlayer(String name) {
    for (ServerThread player : players) if (player.getClientName().equals(name)) return player;
    return null;
  }

  private Map<String, GameBoard> getOpponentBoards(ServerThread player) {
    Map<String, GameBoard> boards = new HashMap<>();
    for (ServerThread opponent : players) {
      if (opponent == player) continue;
      boards.put(opponent.getClientName(), opponent.getGameBoard().getProtectedCopy());
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
    GameBoard tempGameBoard = new GameBoard(gameBoard);
    for (Ship ship : ships) if (!tempGameBoard.placeShip(ship)) return false;
    return true;
  }

  private ServerThread getNextPlayer() {
    if (!playerIterator.hasNext()) playerIterator = players.iterator();
    return playerIterator.next();
  }

  private void placementPhase() { // implement another latch for this? reuse the same latch?
    printGameInfo("Begin placmemnt phase");
    phase = "placement";
    for (ServerThread player : players) {
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
    for (ServerThread player : players) sendGameMessage(player, "The game has started"); //? unnecessary?

    Collections.shuffle(players);

    printGameInfo("The player order is: ");
    for (ServerThread player : players) printGameInfo("  - " + player.getClientName());

    playerIterator = players.iterator();
    currentPlayer = getNextPlayer();

    printGameInfo("The current player is: " + currentPlayer.getClientName());

    sendGameState(currentPlayer, PayloadType.GAME_STATE, String.format("Its %s's turn.", currentPlayer.getClientName()), "It is your turn");

  }

  private synchronized void handleAttack(ServerThread player, Map<String, List<Integer[]>> targetCoordinates) {
    if (currentPlayer == null) currentPlayer = player;
    if (player != currentPlayer) return;

    printGameInfo(player.getClientName() + " is executing their attack (validated)");

    printGameInfo(String.format("\nPlayer Board for %s:\n%s", player.getClientName(), player.getGameBoard().toString()));

    for (String name : targetCoordinates.keySet()) {
      List<Integer[]> coordinates = targetCoordinates.get(name);
      GameBoard targetBoard = getPlayer(name).getGameBoard();
      for (Integer[] coordinate : coordinates) {
        int x = coordinate[0];
        int y = coordinate[1];
        printGameInfo("Targeting " + name + " at " + x + ", " + y);
        if (targetBoard.getPiece(x, y) == PieceType.SHIP) {
          printGameInfo(String.format("%sHit%s", ANSI_RED, ANSI_RESET));
          targetBoard.setPiece(x, y, PieceType.HIT);
          sendGameState(player, PayloadType.MESSAGE, String.format("%s hit one of %s's ships", player.getClientName(), name), String.format("You hit one of %s's ships on %s, %s", name, x+1, y+1));
        } else {
          printGameInfo(String.format("%sMiss%s", ANSI_RED, ANSI_RESET));
          targetBoard.setPiece(x, y, PieceType.MISS);
          sendGameState(player, PayloadType.MESSAGE, String.format("%s missed while targeting %s", player.getClientName(), name), String.format("Your shot at %s on %s, %s missed", name, y+1, x+1));
        }
        printGameInfo(String.format("Target Board for %s:\n%s", name, targetBoard.toString()));
      }
      counterTimer.decrement();
    }
  }

  @Override
  public void run() {
    isRunning = true;
    printGameInfo("Battleship game thread started");
    if (!hardDifficulty) hardDifficulty = false;
    if (!salvoGameMode) salvoGameMode = false;

    printGameInfo("Waiting for players to join");
    counterTimer.runLambdas(); // just a counter that does nothing but wait for up to 4 people to join (or 3 minutes to pass)

    printGameInfo("All players who are going to play have joined");
    printGameInfo("Game thread running");

    counterTimer = new countDown(() -> { gamePhaseInitializer(); }, () -> { 
      gamePhaseInitializer(); 
      for (ServerThread player : players) {
        if (!player.getGameBoard().hasShips()) {
          sendGameMessage(player, "You have not placed your ships");
          addSpectator(player);
          removePlayer(player);
        }
        sendGameMessage(player, "\nTimeout reached, starting game phase, any players who have not placed their ships will be spectators, and any unplaced ships will be lost");
      }
    }, players.size(), 180);

    printGameInfo("Waiting for players to place ships");
    counterTimer.runLambdas(); // waits for all players to place their ships (or 3 minutes to pass)

    printGameInfo("All players who are playing have placed their ships");
    printGameInfo("Game Flow: turns starting");

    while (players.size() > 1) { //? should this be a while loop?
      counterTimer = new countDown(() -> { 
        printGameInfo("The old current player was: " + ANSI_RED + currentPlayer.getClientName() + ANSI_YELLOW + " and they have finished their turn");
        currentPlayer = getNextPlayer();
        sendGameState(currentPlayer, PayloadType.GAME_STATE, String.format("Its %s's turn.", currentPlayer.getClientName()), "It is your turn");
      }, () -> {
        printGameInfo("The old current player was: " + ANSI_RED + currentPlayer.getClientName() + ANSI_YELLOW+ " and they have run out of time");
        sendGameMessage(currentPlayer, "Unfortunatley, you have run out of time, you will be skipped this turn");
        currentPlayer = getNextPlayer();
        sendGameState(currentPlayer, PayloadType.GAME_STATE, String.format("Its %s's turn.", currentPlayer.getClientName()), "It is your turn");
      }, players.size() - 1, 90);

      printGameInfo("Waiting for " + currentPlayer.getClientName() + " to take their turn");
      counterTimer.runLambdas();
    }

    // sendGameEnd();

    isRunning = false;
  }
}

class countDown { // Yes, i know best practice is for a new file, but nothing else will use it
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
    if (timeRemaining < 10) System.out.print( '\r' + BattleshipThread.ANSI_RESET + BattleshipThread.ANSI_RED + "Time remaining: " + timeRemaining + BattleshipThread.ANSI_RESET);
    else if (timeRemaining < 30) System.out.print( '\r' + BattleshipThread.ANSI_RESET + BattleshipThread.ANSI_YELLOW + "Time remaining: " + timeRemaining + BattleshipThread.ANSI_RESET);
    else System.out.print( '\r' + BattleshipThread.ANSI_RESET + BattleshipThread.ANSI_GRAY + "Time remaining: " + timeRemaining + BattleshipThread.ANSI_RESET + "   ");
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

    ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
      long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
      timeRemaining = timeoutSeconds - elapsedTime;
      // if ( ((int) elapsedTime) % 5 == 0) printTimer()  // to regulate how often it prints
        printTimer();
      // System.out.println("Time remaining: " + timeRemaining + " seconds");
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
  }
}