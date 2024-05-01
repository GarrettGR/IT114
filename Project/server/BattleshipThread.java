package Project.server;

import Project.common.*;
import java.util.ArrayList;
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
  private CountDownLatch latch;

  private List<ServerThread> players = new ArrayList<>();
  private List<ServerThread> spectators = new ArrayList<>();

  private final static Map<ShipType, Integer> shipCounts = Map.of(
    ShipType.CARRIER, 1,
    ShipType.BATTLESHIP, 1,
    ShipType.CRUISER, 2,
    ShipType.SUBMARINE, 2,
    ShipType.DESTROYER, 2,
    ShipType.LIFE_BOAT, 0
  );

  public BattleshipThread(Room room, boolean hardDifficulty, boolean salvoGameMode, int playerCount) {
    this.hardDifficulty = hardDifficulty;
    this.salvoGameMode = salvoGameMode;
    this.room = room;
    System.out.println("Battleship game thread created");
    this.latch = new CountDownLatch(playerCount);
  }

  public void sendGameState(ServerThread player, PayloadType type, String message, String privledgedMessage) {
    Payload payload = new Payload();
    payload.setPayloadType(type);
    payload.setClientName(player.getClientName());
    payload.setMessage(message);
    for (ServerThread opponent : players) { 
      if (opponent == player) continue;
      payload.setPlayerBoard(opponent.getGameBoard());
      for (ServerThread other : players) {
        if (other == opponent) continue;
        payload.addOpponentBoard(other.getGameBoard().getProtectedCopy());
      }
      opponent.sendGameEvent(payload);
    }
    payload.setMessage(privledgedMessage);
    payload.setOpponentBoards(getOpponentBoards(player));

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

  public void processPayload(ServerThread player, Payload payload) { // this should be for receiving a payload from a player (?)
    switch (payload.getPayloadType()) {
      case GAME_START ->  addPlayer(player);
      case GAME_PLACE -> {
        if (started || hasPlacedShips(player)) return;
        List<Ship> ships = payload.getShipList();
        if (!validateShipCounts(ships)) {
          sendGameMessage(player, "You cannot place any more ships");
          return;
        }
        if (!validateShipPlacements(ships, player.getGameBoard())) {
          sendGameMessage(player, "Invalid ship placement");
          return;
        }
        for (Ship ship : ships) {
          shipCounts.put(ship.getType(), shipCounts.get(ship.getType()) - 1);
          player.getGameBoard().placeShip(ship);
        }
        sendGameMessage(player, "You have placed your ships, waiting for other players");
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
      }
      default -> throw new IllegalArgumentException("Unexpected value: " + payload.getPayloadType());
    }
  }

  protected synchronized void addPlayer(ServerThread player) { 
    if (!started) {
      players.add(player);
      latch.countDown();
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

  private List<GameBoard> getOpponentBoards(ServerThread player) {
    List<GameBoard> boards = new ArrayList<>();
    for (ServerThread opponent : players) {
      if (opponent == player) continue;
      boards.add(opponent.getGameBoard());
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
    System.out.println("Game starting");
    started = true;

    for (ServerThread player : players) {
      Payload p = new Payload();
      p.setPayloadType(PayloadType.GAME_START);
      p.setClientName("Game");
      p.setMessage("Game starting");
      p.setNumber((int) players.size());
      player.sendGameEvent(p);
    }

    for (ServerThread player : players) {
      Payload p = new Payload();
      p.setPayloadType(PayloadType.GAME_PLACE);
      p.setClientName("Game");
      p.setMessage("Place your ships");
      p.setPlayerBoard(player.getGameBoard());
      for (ShipType type : shipCounts.keySet()) {
        for (int i = 0; i < shipCounts.get(type); i++) {
          Ship ship = new Ship(type);
          p.addShip(ship);
        }
      }
      player.sendGameEvent(p);
    }
  }

  @Override
  public void run() {
    isRunning = true;
    System.out.println("Battleship game thread started");
    if (!hardDifficulty) hardDifficulty = false;
    if (!salvoGameMode) salvoGameMode = false;
    
    try {
      if (!latch.await(300, TimeUnit.SECONDS)) {
        placementPhase(); // if not all players join, start the game with the players that did
      }
      placementPhase(); // if all players join, start the game with all players without waiting
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    isRunning = false;
  }
}