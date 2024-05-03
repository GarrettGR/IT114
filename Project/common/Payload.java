package Project.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Payload implements Serializable {
  private static final long serialVersionUID = 1L;
  private PayloadType payloadType;
  private String clientName;
  private String playerName;
  private String message;
  private long number;
  private boolean isTurn = false;
  private boolean isGameOver = false;
  private Map<String, GameBoard> boards = new HashMap<>(); // Username, GameBoard
  private List<Ship> ships = new ArrayList<>();
  private Map<String,  List<Integer[]>> coordinates = new HashMap<>(); // Username, coordinates
  
  // Overloaded setters and getters is a little overkill... but I also kept wanting to deal with them slightly differently in different places... hopefully no weird errors come from this...

  public PayloadType getPayloadType() { return payloadType; }

  public void setPayloadType(PayloadType payloadType) { this.payloadType = payloadType; }

  public String getClientName() { return clientName; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public String getPlayerName() { return playerName; }

  public void setPlayerName(String playerName) { this.playerName = playerName; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public long getNumber() { return number; }

  public void setNumber(long number) { this.number = number; }

  public void setTurn(boolean isTurn) { this.isTurn = isTurn; }

  public boolean isTurn() { return isTurn; }

  public void setGameOver(boolean isGameOver) { this.isGameOver = isGameOver; }

  public boolean isGameOver() { return isGameOver; }

  public synchronized void setPlayerBoard(GameBoard board) { this.boards.put(playerName, board); }

  public synchronized GameBoard getPlayerBoard() { return this.boards.get(playerName); }

  public synchronized void addOpponentBoard(String key, GameBoard board) { this.boards.put(key, board); }

  public synchronized void setOpponentBoards(Map<String, GameBoard> boards) { this.boards = boards; }

  public synchronized Map<String, GameBoard> getOpponentBoardsMap() {
    return boards.entrySet().stream().filter(entry -> entry.getKey() != null && !entry.getKey().equals(this.playerName)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public synchronized GameBoard[] getOpponentBoards() {
    return boards.entrySet().stream().filter(entry -> entry.getKey() != null && !entry.getKey().equals(this.playerName)).map(Map.Entry::getValue).toArray(GameBoard[]::new);  
  }

  public synchronized List<GameBoard> getOpponentBoardsList() {
    return boards.entrySet().stream().filter(entry -> entry.getKey() != null && !entry.getKey().equals(this.playerName)).map(Map.Entry::getValue).collect(Collectors.toList());
  }

  public synchronized GameBoard getOpponentBoard(String key) { return key.equals(this.playerName) ? boards.get(key) : null; }

  public void addShip(Ship ship) { this.ships.add(ship); }

  public void setShips(List<Ship> ships) { this.ships = ships; }

  public void setShips(Ship[] ships) { this.ships = List.of(ships); }

  public Ship[] getShips() { return ships.toArray(Ship[]::new); }

  public List<Ship> getShipList() { return ships; }

  public Ship getShip(int index) { return ships.get(index); }

  public void setCoordinates(Map<String, List<Integer[]>> coordinates) { this.coordinates = coordinates; }

  public Map<String, List<Integer[]>> getCoordinates() { return coordinates; }

  public List<Integer[]> getCoordinates(String key) { return coordinates.get(key); }

  public void addCoordinate(String key, Integer[] coordinate) { 
    if (coordinates.containsKey(key)) coordinates.get(key).add(coordinate);
    else {
      List<Integer[]> list = new ArrayList<>();
      list.add(coordinate);
      coordinates.put(key, list);
    }
  }

  @Override
  public String toString() {
    return String.format("Type[%s], Number[%s], Message[%s], Name[%s]\nPlayerBoard[%s], OpBoard[%s], ships[%s], Coords[%s]", getPayloadType().toString(), getNumber(), getMessage(), getClientName(), getPlayerBoard() != null ? "true" : "false", getOpponentBoards() != null && !getOpponentBoardsList().isEmpty() ? "true" : "false", getShips() != null && !getShipList().isEmpty() ? "true" : "false", getCoordinates() != null && !getCoordinates().isEmpty() ? "true" : "false");
  }
}