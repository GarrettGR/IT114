package Project.common;

import java.io.Serializable;

public class GameBoard implements Serializable{
  private static final int BOARD_SIZE = 10;
  private PieceType[][] board = new PieceType[BOARD_SIZE][BOARD_SIZE]; //? Make volatile?
  private String clientName;

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_YELLOW = "\u001B[33m";
  private static final String ANSI_GRAY = "\u001B[38;2;150;150;150m";

  public final PieceType[][] getCleanBoard() {
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
    this.board = gameBoard.getBoard();
  }

  public GameBoard() { 
    this(""); 
    this.board = getCleanBoard();
  }

  public int getBoardSize() { return BOARD_SIZE; }

  public synchronized GameBoard getProtectedCopy() {
    GameBoard copy = new GameBoard(clientName);
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        if (board[i][j] == PieceType.SHIP) copy.setPiece(i, j, PieceType.EMPTY);
        else copy.setPiece(i, j, board[i][j]);
    return copy;
  }

  public synchronized PieceType[][] getBoardCopy() {
    PieceType[][] copy = new PieceType[BOARD_SIZE][BOARD_SIZE];
    for (int i = 0; i < BOARD_SIZE; i++)
      System.arraycopy(board[i], 0, copy[i], 0, BOARD_SIZE);
    return copy;
  }

  public synchronized void setBoard(PieceType[][] board) { this.board = board; }

  public synchronized void setBoard(GameBoard gameBoard) { this.setBoard(gameBoard.getBoard()); }

  public synchronized PieceType[][] getBoard() { return board; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public String getClientName() { return clientName; }

  public synchronized void setPiece(int x, int y, PieceType piece) { board[x][y] = piece; }

  public PieceType getPiece(int x, int y) { return board[x][y]; }

  public boolean hasShips() {
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        if (board[i][j] == PieceType.SHIP) return true;
    return false;
  }

  public synchronized  boolean placeShip(Ship ship) {
    PieceType[][] tempBoard = getCleanBoard();
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

    this.setBoard(tempBoard); // if all placements are valid, update the board
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(ANSI_RESET).append(ANSI_YELLOW).append(String.format("%s's board:\n", clientName)).append(ANSI_RESET).append("   1  2  3  4  5  6  7  8  9  10").append('\n');
    int index = 1;
    for (PieceType[] row : board) {
      sb.append(index % 10 != 0 ? (" " + index) : index);
      index++;
      for (PieceType piece : row) {
        sb.append(ANSI_GRAY).append('[').append(piece.toString()).append(ANSI_GRAY).append(']');
      }
      sb.append('\n').append(ANSI_RESET);
    }
    return sb.toString();
  }
}