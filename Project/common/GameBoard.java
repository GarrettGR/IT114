package Project.common;

public class GameBoard {
  private static final int BOARD_SIZE = 10;
  private PieceType[][] board = new PieceType[BOARD_SIZE][BOARD_SIZE];
  private String clientName;

  public GameBoard(String clientName) {
    this.clientName = clientName;
    clearBoard();
  }

  public PieceType[][] getBoard() { return board; }

  public void setBoard(PieceType[][] board) { this.board = board; }

  public String getClientName() { return clientName; }

  public void setClientName(String clientName) { this.clientName = clientName; }

  public void setPiece(int x, int y, PieceType piece) { board[x][y] = piece; }

  public PieceType getPiece(int x, int y) { return board[x][y]; }

  public void clearBoard() {
    for (int i = 0; i < BOARD_SIZE; i++)
      for (int j = 0; j < BOARD_SIZE; j++)
        board[i][j] = PieceType.EMPTY;
  }
}