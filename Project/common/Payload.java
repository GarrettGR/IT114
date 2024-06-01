package Project.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Payload implements Serializable {
  private static final long serialVersionUID = 1L;
  private PayloadType payloadType;
  private String clientName;
  private String roomName;
  private String message;
  private long number;
  private boolean isTurn = false;
  private boolean isGameOver = false;
  private boolean isRename = false;

  private GameBoard playerBoard = new GameBoard();
  private Map<String, GameBoard> opponentBoards = new HashMap<>(); // Username, GameBoard
  private Map<String, PlayerData> playerData = new HashMap<>(); // Username, Player
  private List<Ship> ships = new ArrayList<>();
  private Map<String,  List<Integer[]>> coordinates = new HashMap<>(); // Username, coordinates

  // --- PayloadType (enum) ---
  
  public synchronized PayloadType getPayloadType() { return payloadType; }

  public synchronized void setPayloadType(PayloadType payloadType) { this.payloadType = payloadType; }

  // --- Primitive Types ---

  public synchronized String getClientName() { return clientName; }

  public synchronized void setClientName(String clientName) { this.clientName = clientName; }

  public synchronized String getMessage() { return message; }

  public synchronized void setMessage(String message) { this.message = message; }

  public synchronized String getRoomName() { return roomName; }

  public synchronized void setRoomName(String roomName) { this.roomName = roomName; }

  public synchronized long getNumber() { return number; }

  public synchronized void setNumber(long number) { this.number = number; }

  public synchronized void setTurn(boolean isTurn) { this.isTurn = isTurn; }

  public synchronized void setRename(boolean isRename) { this.isRename = isRename; }

  public synchronized boolean isRename() { return isRename; }

  public synchronized boolean isTurn() { return isTurn; }

  public synchronized void setGameOver(boolean isGameOver) { this.isGameOver = isGameOver; }

  public synchronized boolean isGameOver() { return isGameOver; }

  // --- GameBoard (player) ---

  public synchronized void setPlayerBoard(GameBoard board) { this.playerBoard.setBoard(board); }

  public synchronized GameBoard getPlayerBoard() { return playerBoard; }

  // --- Opponent Boards ---

  public synchronized void addOpponentBoard(String key, GameBoard board) { this.opponentBoards.put(key, board); }

  public synchronized void setOpponentBoards(Map<String, GameBoard> boards) { this.opponentBoards = boards; }

  public synchronized Map<String, GameBoard> getOpponentBoardsMap() { return opponentBoards; }

  public synchronized GameBoard[] getOpponentBoards() { return opponentBoards.values().toArray(GameBoard[]::new); }

  public synchronized List<GameBoard> getOpponentBoardsList() { return new ArrayList<>(opponentBoards.values()); }

  public synchronized GameBoard getOpponentBoard(String key) { return opponentBoards.get(key); }

  // --- PlayerData ---

  public synchronized void addPlayerData(String key, PlayerData player) { this.playerData.put(key, player); }

  public synchronized void setPlayerData(Map<String, PlayerData> players) { this.playerData = players; }

  public synchronized void setPlayerDataWithList(Map<String, Integer[]> players) { 
    for (Map.Entry<String, Integer[]> entry : players.entrySet()) {
      PlayerData player = new PlayerData(entry.getValue()[0]);
      player.setHits(entry.getValue()[1]);
      player.setMisses(entry.getValue()[2]);
      player.setScore(entry.getValue()[3]);
      player.setCurrency(entry.getValue()[4]);
      player.isAway(entry.getValue()[5] == 1);
      player.isTurn(entry.getValue()[6] == 1);
      this.playerData.put(entry.getKey(), player);
    }
  }

  public synchronized Map<String, PlayerData> getPlayerDataMap() { return playerData; }

  public synchronized PlayerData getPlayerData(String key) { return playerData.get(key); }

  public synchronized PlayerData[] getPlayerDataArray() { return playerData.values().toArray(PlayerData[]::new); }

  public synchronized List<PlayerData> getPlayerDataList() { return new ArrayList<>(playerData.values()); }

  // --- Ships ---

  public synchronized void addShip(Ship ship) { this.ships.add(ship); }

  public synchronized void setShips(List<Ship> ships) { this.ships = ships; }

  public synchronized void setShips(Ship[] ships) { this.ships = List.of(ships); }

  public synchronized Ship[] getShips() { return ships.toArray(Ship[]::new); }

  public synchronized List<Ship> getShipList() { return ships; }

  public synchronized Ship getShip(int index) { return ships.get(index); }

  // --- Coordinates ---

  public synchronized void setCoordinates(Map<String, List<Integer[]>> coordinates) { this.coordinates = coordinates; }

  public synchronized Map<String, List<Integer[]>> getCoordinates() { return coordinates; }

  public synchronized List<Integer[]> getCoordinates(String key) { return coordinates.get(key); }

  public synchronized void addCoordinate(String key, Integer[] coordinate) { 
    if (coordinates.containsKey(key)) coordinates.get(key).add(coordinate);
    else {
      List<Integer[]> list = new ArrayList<>();
      list.add(coordinate);
      coordinates.put(key, list);
    }
  }

  @Override
  public String toString() {
    String ANSI_RESET = "\u001B[0m";
    return String.format(
      "Type[%s], Number[%s], Message[%s%s], Name[%s], Rename[%s], PlayerBoard[%s], PlayerData[%s], OpponentBoard[%s], ships[%s], Coords[%s]",
      getPayloadType().toString(),
      getNumber(), 
      getMessage(), 
      ANSI_RESET,
      getClientName(), 
      isRename(), 
      getPlayerBoard().getBoard() != GameBoard.getCleanBoard() ? "true" : "false", 
      getPlayerDataList() != null && !getPlayerDataList().isEmpty() ? "true" : "false",
      getOpponentBoards() != null && !getOpponentBoardsList().isEmpty() ? "true" : "false", 
      getShips() != null && !getShipList().isEmpty() ? "true" : "false", 
      getCoordinates() != null && !getCoordinates().isEmpty() ? "true" : "false");
  }
}