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
  private String message;
  private long number;
  private GameBoard playerBoard;
  private List<GameBoard> opponentBoards = new ArrayList<>();
  private List<Ship> ships = new ArrayList<>();
  private Map<String,  List<Integer[]>> coordinates = new HashMap<>();

  public PayloadType getPayloadType() { return payloadType; }

  // Overloaded setters and getters is a little overkill... but I also kept wanting to deal with them slightly differently in different places... hopefully no weird errors come from this...

  public void setPayloadType(PayloadType payloadType) { this.payloadType = payloadType; }

  public String getClientName() { return clientName; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public long getNumber() { return number; }

  public void setNumber(long number) { this.number = number; }

  public void setPlayerBoard(GameBoard board) { this.playerBoard = board; }

  public GameBoard getPlayerBoard() { return playerBoard; }

  public void addOpponentBoard(GameBoard board) { this.opponentBoards.add(board); }

  public void setOpponentBoards(List<GameBoard> boards) { this.opponentBoards = boards; }

  public void setOpponentBoards(GameBoard[] boards) { this.opponentBoards = List.of(boards); } // rewrote to not need this... remove?

  public GameBoard[] getOpponentBoards() { return opponentBoards.toArray(GameBoard[]::new); }

  public List<GameBoard> getOpponentBoardsList() { return opponentBoards; }

  public GameBoard getOpponentBoard(int index) { return opponentBoards.get(index); }

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