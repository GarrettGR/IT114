package Project.client;

import Project.common.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class GamePanel extends JPanel { // Change to JSplitPane?
  private List<GameBoard> boards = new ArrayList<>();

  public GamePanel(List<GameBoard> boards) {
    setLayout(new GridLayout(boards.size() > 2 ? 2 : 1, 2));

    for (GameBoard board : boards) {
      PlayerPanel playerPanel = new PlayerPanel(board);
      add(playerPanel);
    }
  }
}

class PlayerPanel extends JPanel {
  private GameBoard gameBoard;

  public PlayerPanel(GameBoard gameBoard) {
    this.gameBoard = gameBoard;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    
    // Draw grid
    int cellSize = Math.min(getWidth() / 10, getHeight() / 10);
    for (int row = 0; row < 10; row++) {
      for (int col = 0; col < 10; col++) {
        int x = col * cellSize;
        int y = row * cellSize;
        g.drawRect(x, y, cellSize, cellSize);
      }
    }
    
    // Draw clientName centered over the top of the GameBoard
    String clientName = gameBoard.getClientName();
    FontMetrics fm = g.getFontMetrics();
    int textWidth = fm.stringWidth(clientName);
    int textHeight = fm.getHeight();
    int centerX = getWidth() / 2;
    int centerY = textHeight;
    g.drawString(clientName, centerX - textWidth / 2, centerY);
  }
}