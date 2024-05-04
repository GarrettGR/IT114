package Project.client;

import Project.common.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;


public class GUI {
  private JPanel gamePanel = new JPanel();
  private JPanel[] gameBoards;
  private JButton[][][] gridButtons;
  private UserPanel[] userPanels;
  private ChatPanel chatPanel;

  public static void main(String args[]) {
    JFrame frame = new JFrame("Battleship Game");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 600);
    frame.setLayout(new BorderLayout());



    frame.setVisible(true);
  }

  public void drawGameBoard(GameBoard gameBoard, int boardIndex) {
    PieceType[][] board = gameBoard.getBoard();
    for (int i = 0; i < 10; i++)
      for (int j = 0; j < 10; j++)
        switch (board[i][j]) {
          case SHIP -> gridButtons[boardIndex][i][j].setBackground(Color.WHITE);
          case EMPTY -> gridButtons[boardIndex][i][j].setBackground(Color.GRAY);
          case HIT -> gridButtons[boardIndex][i][j].setBackground(Color.RED);
          case MISS -> gridButtons[boardIndex][i][j].setBackground(Color.YELLOW);
        }
  }

  public void drawGameBoards(List<GameBoard> gameBoardList) {
    int numPlayers = gameBoardList.size();
    gamePanel.setLayout(new GridLayout(1, numPlayers));
    gameBoards = new JPanel[numPlayers];
    gridButtons = new JButton[numPlayers][10][10];

    for (int i = 0; i < numPlayers; i++) {
      gameBoards[i] = new JPanel();
      gameBoards[i].setLayout(new GridLayout(10, 10));
      gamePanel.add(gameBoards[i]);

      for (int j = 0; j < 10; j++)
        for (int k = 0; k < 10; k++) {
          gridButtons[i][j][k] = new JButton();
          gameBoards[i].add(gridButtons[i][j][k]);
        }

      drawGameBoard(gameBoardList.get(i), i);
    }
  }
}
