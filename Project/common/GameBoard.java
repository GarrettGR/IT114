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
    int anchorX = ship.getAnchorX();
    int anchorY = ship.getAnchorY();
    if (anchorX < 0 || anchorX >= BOARD_SIZE || anchorY < 0 || anchorY >= BOARD_SIZE) return false;
    String orientation = ship.getOrientation().toLowerCase();
    int length = ship.getType().getLength();
    for (int i = 0; i < length; i++) {
      switch (orientation) {
        case "right" -> {
          if (anchorX + i >= BOARD_SIZE) return false;
          if (this.board[anchorX + i][anchorY] == PieceType.SHIP) return false;
          this.board[anchorX + i][anchorY] = PieceType.SHIP;
          return true;
          }
        case "down" -> {
          if (anchorY - i < 0) return false;
          if (this.board[anchorX][anchorY - i] == PieceType.SHIP) return false;
          this.board[anchorX][anchorY - i] = PieceType.SHIP;
          return true;
        }
        case "left" -> {
          if (anchorX - i < 0) return false;
          if (this.board[anchorX - i][anchorY] == PieceType.SHIP) return false;
          this.board[anchorX - i][anchorY] = PieceType.SHIP;
          return true;
        }
        case "up" -> {
          if (anchorY + i >= BOARD_SIZE) return false;
          if (this.board[anchorX][anchorY + i] == PieceType.SHIP) return false;
          this.board[anchorX][anchorY + i] = PieceType.SHIP;
          return true;
        }
      }
    }
    return false;
  }
}