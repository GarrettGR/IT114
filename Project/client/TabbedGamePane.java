package Project.client;

import Project.common.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;

public class TabbedGamePane extends JTabbedPane {
  protected HashMap<Integer, GamePanel> gamePanels = new HashMap<>();

  public TabbedGamePane(HashMap<String, GameBoard> players) {
    setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 5));
    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

    int index = 0;
    for (GameBoard board : players.values()) {
      GamePanel tempGamePanel = new GamePanel(board);
      gamePanels.put(index, tempGamePanel);
      addTab(board.getClientName(), tempGamePanel);
      setMnemonicAt(index, KeyEvent.VK_1 + index);
      index++;
    }
  
    for (int i = 0; i < getTabCount(); i++) {
      JLabel tabLabel = new JLabel(getTitleAt(i));
      if (i == 0) tabLabel.setForeground(Color.YELLOW);
      tabLabel.setHorizontalAlignment(SwingConstants.CENTER);
      tabLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
      setTabComponentAt(i, tabLabel);
    }
  
    addChangeListener(e -> {
      int selectedIndex = getSelectedIndex();
      for (int i = 0; i < getTabCount(); i++) {
        JLabel tabLabel = getTabLabel(i);
        if (i == selectedIndex) {
          System.out.println("Selected tab: " + getTitleAt(i));
          tabLabel.setForeground(Color.YELLOW);
          setTabComponentAt(i, tabLabel);
        } else {
          tabLabel.setForeground(Color.WHITE);
          setTabComponentAt(i, tabLabel);
        }
      }
    });
  }

  public JLabel getTabLabel(int index) { return (JLabel) getTabComponentAt(index); }

  public void updateGamePanels(HashMap<String, GameBoard> boards) {
    int index = 0;
    for (GameBoard board : boards.values()) {
      gamePanels.get(index).setGameBoard(board);
    }
  }
}

class GamePanel extends JPanel {
  private GameBoard gameBoard;

  public GamePanel(GameBoard gameBoard) { this.gameBoard = gameBoard; }

  public void setGameBoard(GameBoard gameBoard) {
    this.gameBoard = gameBoard;
    repaint();
  }

  public GameBoard getGameBoard() { return gameBoard; }

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
  }
}