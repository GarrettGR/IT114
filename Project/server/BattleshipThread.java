package Project.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.util.concurrent.Callable;

import Project.common.*;

public class BattleshipThread extends Thread {
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

  private Map<ServerThread, PieceType[][]> players = Map.of();
  private List<ServerThread> spectators = new ArrayList<>();

  private Map<String, Integer> ShipLengths = Map.of("Carrier", 5, "Battleship", 4, "Cruiser", 3, "Submarine", 3, "Destroyer", 2);
  private Map<String, Integer> ShipCounts = Map.of("Carrier", 1, "Battleship", 1, "Cruiser", 2, "Submarine", 2, "Destroyer", 2);

  public BattleshipThread(boolean hardDifficulty, boolean salvoGameMode) {
    this.hardDifficulty = hardDifficulty;
    this.salvoGameMode = salvoGameMode;
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
    if (!hardDifficulty) hardDifficulty = false;
    if (!salvoGameMode) salvoGameMode = false;

    initBoards();

    // TODO: give all players a chance to place their ships (90 seconds or when all players have placed)

    Callable<Boolean> playerPlaceShip = () -> {
      Payload p = new Payload();
      StringBuilder message = new StringBuilder("Place your ships: ");
      for (String ship : ShipCounts.keySet())
        message.append(String.format("%s(%d), ", ship, ShipCounts.get(ship)));
      p.setPayloadType(PayloadType.GAME_PLACE);
      p.setMessage(message.toString());
      p.setPlayerBoard(EMPTY_BOARD);
      return false;
    };

    for (ServerThread player : players.keySet()) {
      TimedEvent placeShip = new TimedEvent(playerPlaceShip, () -> {
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
          return false;
        };

        TimedEvent takeTurn = new TimedEvent(playerTakeTurn, () -> {
          // TODO: Tell the player they took too long to take their turn (skipped)
        }, 90);
      }

      for (ServerThread player : players.keySet()) {
        boolean noShips = hasPlacedShips(player);
        if (noShips) {
          addSpectator(player);
          removePlayer(player);
        }
      }

      if (players.size() == 1) {
        for (ServerThread player : players.keySet()) {
          // tell the winning player they won
          // tell all the other players who won (and wish them better luck next time)
        }
      }

    }
  }
}