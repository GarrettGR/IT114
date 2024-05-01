package Project.server;

import Project.common.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BattleshipThread extends Thread {
  private final Room room;
  private boolean hardDifficulty;
  private boolean salvoGameMode;
  private boolean started;
  private boolean isRunning = false;

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

  public BattleshipThread(Room room, boolean hardDifficulty, boolean salvoGameMode) {
    this.hardDifficulty = hardDifficulty;
    this.salvoGameMode = salvoGameMode;
    this.room = room;
    System.out.println("Battleship game thread created");
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

  private void processPayload(ServerThread player, Payload payload) { // this should be for receiving a payload from a player (?)
    switch (payload.getPayloadType()) {
      case GAME_START -> {
        if (started) return;
        if (players.size() < 2) {
          Payload p = new Payload();
          p.setPayloadType(PayloadType.MESSAGE);
          p.setClientName("Game");
          p.setMessage("Not enough players to start the game");
          player.sendGameEvent(p);
          return;
        }
        started = true;
      }
      case GAME_PLACE -> {
        if (!started || hasPlacedShips(player)) return;
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
        if (!started || !hasPlacedShips(player)) return;
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

  protected synchronized void addPlayer(ServerThread player) { if (!started) players.add(player); }

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

  @Override
  public void run() {
    isRunning = true;
    System.out.println("Battleship game thread started");
    if (!hardDifficulty) hardDifficulty = false;
    if (!salvoGameMode) salvoGameMode = false;

    // TODO: give all players a chance to place their ships (90 seconds or when all players have placed)

    // for (ServerThread player : players.keySet()) {
    //   Callable<Boolean> playerPlaceShip = () -> {
    //     Payload p = new Payload();
    //     p.setPayloadType(PayloadType.GAME_PLACE);
    //     StringBuilder message = new StringBuilder("Place your ships: ");
    //     message.append("ShipName : [Length, Quantity]");
    //     for (String ship : ShipData.keySet())
    //       message.append(String.format("%s : %d, ", ship, ShipData.get(ship))); // TODO: Format better (easier to read)
    //     p.setMessage(message.toString());
    //     p.setPlayerBoard(EMPTY_BOARD);
    //     List<Object> data = new ArrayList<>();
    //     for (Map.Entry<String, Integer[]> entry : ShipData.entrySet()) {
    //         data.add(entry.getKey());
    //         data.add(entry.getValue());
    //     }
    //     p.setOtherData(data.toArray());
    //     player.sendGameEvent(p);
    //     while (!hasPlacedShips(player)) {
    //       try {
    //         Thread.sleep(500);
    //       } catch (InterruptedException e) {
    //         e.printStackTrace();
    //       }
    //     }
    //     return true;
    //   };
    //   TimedEvent placeShip = new TimedEvent(playerPlaceShip, () -> {
    //     // TODO: Query for a gamestate? (even if not all ships have been placed, those that have will be used)
    //     addSpectator(player);
    //     removePlayer(player);
    //   }, 90);
    //   placeShip.start();
    // }

    // Random random = new Random();
    // List<ServerThread> playerOrder = new ArrayList<>(players.keySet());
    // Collections.shuffle(playerOrder, random);

    // while (isRunning) {
    //   if (players.size() == 0) isRunning = false;
    //   for (ServerThread player : playerOrder) {
    //     Callable<Boolean> playerTakeTurn = () -> {
    //       Payload p = new Payload();
    //       p.setPayloadType(PayloadType.GAME_STATE);
    //       p.setPlayerBoard(players.get(player));
    //       for (ServerThread opponent : players.keySet())
    //         if (opponent != player) p.addOpponentBoard(opponent.getClientName(), players.get(opponent));
    //       p.setMessage("Take your turn");
    //       player.sendGameEvent(p);

    //       // TODO: Wait for the player to take their turn, return true if they took their turn

    //       return true;
    //     };

    //     TimedEvent takeTurn = new TimedEvent(playerTakeTurn, () -> {
    //       Payload p = new Payload();
    //       p.setPayloadType(PayloadType.GAME_MESSAGE);
    //       p.setMessage("You took too long to take your turn");
    //       player.sendGameEvent(p);
    //     }, 90);
    //     takeTurn.start();
    //   }
    //   for (ServerThread player : players.keySet()) {
    //     boolean noShips = hasPlacedShips(player);
    //     if (noShips) {
    //       addSpectator(player);
    //       removePlayer(player);
    //     }
    //   }
    //   if (players.size() == 1) {
    //     ServerThread winner = players.keySet().iterator().next();
    //     for (ServerThread spectator : spectators) {
    //       Payload p = new Payload();
    //       p.setPayloadType(PayloadType.GAME_MESSAGE);
    //       p.setMessage(winner.getClientName() + " has won the game!");
    //       spectator.sendGameEvent(p);
    //     }
    //     Payload p = new Payload();
    //     p.setPayloadType(PayloadType.GAME_MESSAGE);
    //     p.setMessage("You have won the game!");
    //     winner.sendGameEvent(p);
    //     System.out.println("Game over, " + winner.getClientName() + " has won the game!");
    //     isRunning = false;
    //   }
    // }
  }
}