package Project.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Payload implements Serializable {
  private static final long serialVersionUID = 1L;
  private PayloadType payloadType;
  private String clientName;
  private String message;
  private long number;
  private PieceType[][] playerBoard;
  private Map<String, PieceType[][]> opponentBoards = new HashMap<>();
  private Map<String, int[][]> positionMap = new HashMap<>();
  private Object[] otherData; // When placing ships, the 'otherData' will be the list of ships to place (from server to client), then the orientations of those ships (when returning to the server)

  public PayloadType getPayloadType() { return payloadType; }

  public void setPayloadType(PayloadType payloadType) { this.payloadType = payloadType; }

  public String getClientName() { return clientName; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public long getNumber() { return number; }

  public void setNumber(long number) { this.number = number; }

  public void setPlayerBoard(PieceType[][] board) { this.playerBoard = board; }

  public PieceType[][] getPlayerBoard() { return playerBoard; }

  public void addOpponentBoard(String playerName, PieceType[][] board) { this.opponentBoards.put(playerName, board); }

  public void setOpponentBoard(String playerName, PieceType[][] board) { this.opponentBoards.replace(playerName, board); }

  public PieceType[][] getOpponentBoard(String playerName) { return opponentBoards.get(playerName); }

  public Object[] getOpponentBoards() { return opponentBoards.values().toArray(); }

  public void setPosition(String target, int[][] position) { this.positionMap.put(target, position); }

  public int[][] getPosition(String target) { return positionMap.get(target); }

  public void setOtherData(Object[] data) { this.otherData = data; }

  public Object[] getOtherData() { return otherData; }

  @Override
  public String toString() {
     return String.format("Type[%s], Number[%s], Message[%s]", getPayloadType().toString(), getNumber(), getMessage());
  }
}