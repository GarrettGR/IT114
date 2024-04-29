package Project.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Callable;

import Project.common.*;

public class BattleshipThread extends Thread {
  private Room room;
  private static final int BOARD_SIZE = 10;
  private boolean hardDifficulty;
  private boolean salvoGameMode;
  private boolean started;
  private boolean isRunning = false;
  private static final PieceType[][] EMPTY_BOARD = new PieceType[BOARD_SIZE][BOARD_SIZE];

  static {
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        EMPTY_BOARD[i][j] = PieceType.EMPTY;
  }

  private Map<ServerThread, PieceType[][]> players = new HashMap<>();
  private List<ServerThread> spectators = new ArrayList<>();

  private Map<String, Integer[]> ShipData = Map.of(
    "Carrier",    new Integer[] { 5, 1 },
    "Battleship", new Integer[] { 4, 1 },
    "Cruiser",    new Integer[] { 3, 2 },
    "Submarine",  new Integer[] { 3, 2 },
    "Destroyer",  new Integer[] { 2, 2 }
  );

  public BattleshipThread(Room room, boolean hardDifficulty, boolean salvoGameMode) {
    this.hardDifficulty = hardDifficulty;
    this.salvoGameMode = salvoGameMode;
    this.room = room;
    System.out.println("Battleship game thread created");
  }

  private void initBoards() {
    for (ServerThread player : players.keySet()) {
      PieceType[][] board = new PieceType[BOARD_SIZE][BOARD_SIZE];
      for (int i = 0; i < BOARD_SIZE; i++)
        for (int j = 0; j < BOARD_SIZE; j++)
          board[i][j] = PieceType.EMPTY;
      players.put(player, board);
    }
  }

  private void processPayload(ServerThread player, Payload payload) {
    switch (payload.getPayloadType()) {
      case GAME_START:
      case GAME_PLACE:
      case GAME_TURN:
      default:
        break;
    }
  }

  public void pushPayload(ServerThread player, Payload payload) { processPayload(player, payload); }

  protected synchronized void addPlayer(ServerThread player) {
    if (!started) players.put(player, EMPTY_BOARD);
  }

  protected synchronized void addSpectator(ServerThread spectator) { spectators.add(spectator); }

  protected synchronized void removePlayer(ServerThread player) { players.remove(player); }

  protected synchronized void removeSpectator(ServerThread spectator) { spectators.remove(spectator); }

  protected boolean hasPlayer(ServerThread player) { return players.keySet().contains(player); }

  protected boolean hasPlayer(String name) {
    for (ServerThread player : players.keySet()) if (player.getClientName().equals(name)) return true;
    return false;
  }

  protected boolean hasSpectator(ServerThread spectator) { return spectators.contains(spectator); }

  protected boolean hasSpectator(String name) {
    for (ServerThread spectator : spectators) if (spectator.getClientName().equals(name)) return true;
    return false;
  }

  private boolean hasPlacedShips(ServerThread player) {
    for (PieceType[] row : players.get(player))
      for (PieceType piece : row)
        if (piece != PieceType.EMPTY) return true;
    return false;
  }

  @Override
  public void run() {
    isRunning = true;
    System.out.println("Battleship game thread started");
    if (!hardDifficulty) hardDifficulty = false;
    if (!salvoGameMode) salvoGameMode = false;

    initBoards();

    // TODO: give all players a chance to place their ships (90 seconds or when all players have placed)

    for (ServerThread player : players.keySet()) {
      Callable<Boolean> playerPlaceShip = () -> {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.GAME_PLACE);
        StringBuilder message = new StringBuilder("Place your ships: ");
        message.append("ShipName : [Length, Quantity]");
        for (String ship : ShipData.keySet())
          message.append(String.format("%s : %d, ", ship, ShipData.get(ship))); // TODO: Format better (easier to read)
        p.setMessage(message.toString());
        p.setPlayerBoard(EMPTY_BOARD);
        List<Object> data = new ArrayList<>();
        for (Map.Entry<String, Integer[]> entry : ShipData.entrySet()) {
            data.add(entry.getKey());
            data.add(entry.getValue());
        }
        p.setOtherData(data.toArray());
        player.sendGameEvent(p);
        while (!hasPlacedShips(player)) {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        return true;
      };
      TimedEvent placeShip = new TimedEvent(playerPlaceShip, () -> {
        // TODO: Query for a gamestate? (even if not all ships have been placed, those that have will be used)
        addSpectator(player);
        removePlayer(player);
      }, 90);
      placeShip.start();
    }

    Random random = new Random();
    List<ServerThread> playerOrder = new ArrayList<>(players.keySet());
    Collections.shuffle(playerOrder, random);

    while (isRunning) {
      if (players.size() == 0) isRunning = false;
      for (ServerThread player : playerOrder) {
        Callable<Boolean> playerTakeTurn = () -> {
          Payload p = new Payload();
          p.setPayloadType(PayloadType.GAME_STATE);
          p.setPlayerBoard(players.get(player));
          for (ServerThread opponent : players.keySet())
            if (opponent != player) p.addOpponentBoard(opponent.getClientName(), players.get(opponent));
          p.setMessage("Take your turn");
          player.sendGameEvent(p);

          // TODO: Wait for the player to take their turn, return true if they took their turn

          return true;
        };

        TimedEvent takeTurn = new TimedEvent(playerTakeTurn, () -> {
          Payload p = new Payload();
          p.setPayloadType(PayloadType.GAME_MESSAGE);
          p.setMessage("You took too long to take your turn");
          player.sendGameEvent(p);
        }, 90);
        takeTurn.start();
      }
      for (ServerThread player : players.keySet()) {
        boolean noShips = hasPlacedShips(player);
        if (noShips) {
          addSpectator(player);
          removePlayer(player);
        }
      }
      if (players.size() == 1) {
        ServerThread winner = players.keySet().iterator().next();
        for (ServerThread spectator : spectators) {
          Payload p = new Payload();
          p.setPayloadType(PayloadType.GAME_MESSAGE);
          p.setMessage(winner.getClientName() + " has won the game!");
          spectator.sendGameEvent(p);
        }
        Payload p = new Payload();
        p.setPayloadType(PayloadType.GAME_MESSAGE);
        p.setMessage("You have won the game!");
        winner.sendGameEvent(p);
        System.out.println("Game over, " + winner.getClientName() + " has won the game!");
        isRunning = false;
      }
    }
  }
}