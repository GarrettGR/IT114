package Project.server;

import Project.common.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
  private List<ServerThread> spectators = new ArrayList<>();

  // private Map<ShipType, Integer> shipCounts = Map.of(
  //     ShipType.CARRIER, 1,
  //     ShipType.BATTLESHIP, 1,
  //     ShipType.CRUISER, 2,
  //     ShipType.SUBMARINE, 2,
  //     ShipType.DESTROYER, 2,
  //     ShipType.LIFE_BOAT, 0
  //   );

  private Map<ShipType, Integer> shipCounts = new HashMap<ShipType, Integer>() {{
    put(ShipType.CARRIER, 1);
    put(ShipType.BATTLESHIP, 1);
    put(ShipType.CRUISER, 1);
    put(ShipType.SUBMARINE, 1);
    put(ShipType.DESTROYER, 0);
    put(ShipType.LIFE_BOAT, 0);
  }};

  public BattleshipThread(Room room, boolean hardDifficulty, boolean salvoGameMode, int playerCount) {
    this.hardDifficulty = hardDifficulty;
    this.salvoGameMode = salvoGameMode;
    this.room = room;
    System.out.println("Battleship game thread created");
    counterTimer = new countDown(() -> { placementPhase(); }, () -> { placementPhase(); }, playerCount, 300);
  }

  public void sendGameState(ServerThread player, PayloadType type, String message, String privledgedMessage) {
    Payload payload = new Payload();
    payload.setPayloadType(type);
    payload.setClientName(player.getClientName());
    payload.setMessage(message);
    for (ServerThread opponent : players) { 
      if (opponent == player) continue;
      if (currentPlayer != null && opponent == currentPlayer) payload.setTurn(true);
      else payload.setTurn(false);
      payload.setPlayerBoard(opponent.getGameBoard());
      for (ServerThread other : players) {
        if (other == opponent) continue;
        payload.addOpponentBoard(other.getClientName(), other.getGameBoard().getProtectedCopy());
      }
      opponent.sendGameEvent(payload);
    }
    if (player == currentPlayer) payload.setTurn(true);
    else payload.setTurn(false);
    payload.setMessage(privledgedMessage);
    payload.setOpponentBoards(getOpponentBoards(player));
    payload.setPlayerBoard(player.getGameBoard());
    player.sendGameEvent(payload);

    for (ServerThread p : players) if (player != p) payload.addOpponentBoard(p.getClientName(), p.getGameBoard()); // let spectators see all boards
    for (ServerThread spec : spectators) spec.sendGameEvent(payload);
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
        if (started || hasPlacedShips(player)) return;
        System.out.println("Placing ships");
        List<Ship> ships = payload.getShipList();
        // if (!validateShipCounts(ships)) {
        //   System.err.println("Invalid ship counts");
        //   sendGameMessage(player, "You cannot place any more ships");
        //   return;
        // }
        if (!validateShipPlacements(ships, player.getGameBoard())) {
          System.out.println("Invalid ship placement");
          sendGameMessage(player, "Invalid ship placement");
          return;
        }
        GameBoard board = player.getGameBoard() != null ? new GameBoard(player.getGameBoard()) : new GameBoard();
        for (Ship ship : ships) board.placeShip(ship);
        player.setGameBoard(board);
        System.out.println("Ships placed successfully for " + player.getClientName()); 
        sendGameMessage(player, "You have placed your ships, waiting for other players");
        counterTimer.decrement(); // TODO: check if I should create a whole new latch for this
      }
      case GAME_TURN -> {
        if (!started || !hasPlacedShips(player)) return; // make them a spectator -- send a message they can't do that?
        Map<String, List<Integer[]>> targetCoordinates = payload.getCoordinates();
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
        takeTurn(player, targetCoordinates);
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
    Map<String, GameBoard> boards = new HashMap();
    for (ServerThread opponent : players) {
      if (opponent == player) continue;
      boards.put(opponent.getClientName(), opponent.getGameBoard().getProtectedCopy());
    }
    return boards;
  }

  private boolean hasPlacedShips(ServerThread player) {
    if (player.getGameBoard() == null) return false;
    GameBoard board = player.getGameBoard();
    for (int i = 0; i < board.getBoardSize(); i++)
      for (int j = 0; j < board.getBoardSize(); j++)
        if (board.getPiece(i, j) == PieceType.SHIP) return true;
    return false;
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

  private void placementPhase() { // implement another latch for this? reuse the same latch?
    System.out.println("Begin placmemnt phase");  
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
      player.sendGameEvent(p);
    }
    // wait until all players have placed their ships or 3 minutes have passed
    counterTimer = new countDown(() -> { gamePhase(); }, () -> { gamePhase(); }, players.size(), 180);
    counterTimer.runLambdas();
  }

  private void gamePhase() {
    System.out.println("Begin game phase");
    phase = "game";
    started = true;
    for (ServerThread player : players) sendGameMessage(player, "The game has started"); //? unnecessary?

    Collections.shuffle(players);

    System.out.println("The player order is: ");
    for (ServerThread player : players) System.out.println("  - " + player.getClientName());

    while (players.size() > 1) {
      
      

    }

    // sendGameEnd();

  }

  private synchronized void takeTurn(ServerThread player, Map<String, List<Integer[]>> targetCoordinates) {
    if (currentPlayer == null) currentPlayer = player;
    if (player != currentPlayer) return;

    System.out.println(player.getClientName() + " is taking their turn");

    for (String name : targetCoordinates.keySet()) {
      List<Integer[]> coordinates = targetCoordinates.get(name);
      GameBoard targetBoard = getPlayer(name).getGameBoard();
      for (Integer[] coordinate : coordinates) {
        int x = coordinate[0];
        int y = coordinate[1];
        System.out.println("Targeting " + name + " at " + x + ", " + y);
        if (targetBoard.getPiece(x, y) == PieceType.SHIP) {
          System.out.println("Hit");
          targetBoard.setPiece(x, y, PieceType.HIT);
          sendGameState(player, PayloadType.GAME_TURN, String.format("%s hit one of %s's ships", player.getClientName(), name), String.format("You hit one of %s's ships on %s, %s", name, x, y));
        } else {
          System.out.println("Miss");
          targetBoard.setPiece(x, y, PieceType.MISS);
          sendGameState(player, PayloadType.GAME_TURN, String.format("%s missed while targeting %s", player.getClientName(), name), String.format("Your shot at %s on %s, %s missed", name, x, y));
        }
      }
    }

    int currentPlayerIndex = players.indexOf(currentPlayer);
    currentPlayer = players.get((currentPlayerIndex + 1) % players.size());
  }

  @Override
  public void run() {
    isRunning = true;
    System.out.println("Battleship game thread started");
    if (!hardDifficulty) hardDifficulty = false;
    if (!salvoGameMode) salvoGameMode = false;

    counterTimer.runLambdas();

    System.out.println("Game thread running");

    isRunning = false;
  }
}

class countDown {
  private final Runnable lambda1;
  private final Runnable lambda2;
  private final int timeoutSeconds;
  private CountDownLatch latch;

  public countDown(Runnable lambda1, Runnable lambda2, int latchCount, int timeoutSeconds) {
    this.lambda1 = lambda1;
    this.lambda2 = lambda2;
    this.timeoutSeconds = timeoutSeconds;
    this.latch = new CountDownLatch(latchCount);
    System.out.println("Countdown created");
  }

  public void decrement() {
    System.out.println("Countdown decremented");
    latch.countDown();
  }

  public void runLambdas() {;
    Thread counter = new Thread(() -> {
      try {
        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
          System.out.println("Timeout occurred");
          lambda2.run();
        } else {
          System.out.println("Countdown completed");
          lambda1.run();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    counter.start();
  }
}