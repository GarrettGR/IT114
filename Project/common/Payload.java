package Project.common;

import java.io.Serializable;
import java.util.Map;

public class Payload implements Serializable {
  private static final long serialVersionUID = 1L;
  private PayloadType payloadType;
  private String clientName;
  private String message;
  private int number;
  private PieceType[][] playerBoard;
  private Map<String, PieceType[][]> opponentBoards = Map.of();
  private int[][] position;

  public PayloadType getPayloadType() { return payloadType; }

  public void setPayloadType(PayloadType payloadType) { this.payloadType = payloadType; }

  public String getClientName() { return clientName; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public String getMessage() { return message; }

  public void setMessage(String message) { this.message = message; }

  public int getNumber() { return number; }

  public void setNumber(int number) { this.number = number; }

  public void setPlayerBoard(PieceType[][] board) { this.playerBoard = board; }

  public PieceType[][] getPlayerBoard() { return playerBoard; }

  public void addOpponentBoard(String playerName, PieceType[][] board) { this.opponentBoards.put(playerName, board); }

  public void setOpponentBoard(String playerName, PieceType[][] board) { this.opponentBoards.replace(playerName, board); }

  public PieceType[][] getOpponentBoard(String playerName) { return opponentBoards.get(playerName); }

  public Object[] getOpponentBoards() { return opponentBoards.values().toArray(); }

  public void setPosition(int[][] position) { this.position = position; }

  public int[][] getPosition() { return position; }

  @Override
  public String toString() {
     return String.format("Type[%s], Number[%s], Message[%s]", getPayloadType().toString(), getNumber(), getMessage());
  }
}