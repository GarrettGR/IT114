package Project.common;

import java.io.Serializable;

public class GameBoard implements Serializable{
  private static final int BOARD_SIZE = 10;
  private PieceType[][] board = new PieceType[BOARD_SIZE][BOARD_SIZE];
  private String clientName;

  public static PieceType[][] getCleanBoard() {
    PieceType cleanBoard[][] = new PieceType[BOARD_SIZE][BOARD_SIZE];
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        cleanBoard[i][j] = PieceType.EMPTY;
    return cleanBoard;
  }

  public GameBoard(String clientName) {
    this.clientName = clientName;
    this.board = getCleanBoard();
  }

  public GameBoard(GameBoard gameBoard) {
    this.clientName = gameBoard.clientName;
    this.board = gameBoard.board;
  }

  public GameBoard() {
    this("");
  }

  public int getBoardSize() { return BOARD_SIZE; }

  public PieceType[][] getBoard() { return board; }

  public GameBoard getProtectedCopy() {
    GameBoard copy = new GameBoard(clientName);
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        if (board[i][j] == PieceType.SHIP) copy.setPiece(i, j, PieceType.EMPTY);
        else copy.setPiece(i, j, board[i][j]);
    return copy;
  }

  public void setBoard(PieceType[][] board) { this.board = board; }

  public String getClientName() { return clientName; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public void setPiece(int x, int y, PieceType piece) { board[x][y] = piece; }

  public PieceType getPiece(int x, int y) { return board[x][y]; }

  public boolean placeShip(Ship ship) {
    PieceType[][] tempBoard = new PieceType[BOARD_SIZE][BOARD_SIZE];
    for (int i = 0; i < BOARD_SIZE; i++) System.arraycopy(this.board[i], 0, tempBoard[i], 0, BOARD_SIZE);

    int anchorX = ship.getAnchorX();
    int anchorY = ship.getAnchorY();
    String orientation = ship.getOrientation().toLowerCase();
    int length = ship.getType().getLength();

    for (int i = 0; i < length; i++) {
      int x = anchorX;
      int y = anchorY;
      switch (orientation) {
        case "right" -> y += i;
        case "down" -> x += i;
        case "left" -> y -= i;
        case "up" -> x -= i;
        default -> { return false; }
      }
      if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE || tempBoard[x][y] == PieceType.SHIP) return false;
      tempBoard[x][y] = PieceType.SHIP;
    }
    this.board = tempBoard; // if all placements are valid, update the board
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("   1  2  3  4  5  6  7  8  9  10").append('\n');
    int index = 1;
    for (PieceType[] row : board) {
      sb.append(index % 10 != 0 ? (" " + index) : index);
      index++;
      for (PieceType piece : row) {
        sb.append('[').append(piece.toString()).append(']');
      }
      sb.append('\n');
    }
    return sb.toString();
  }
}