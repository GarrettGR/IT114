package Project.client;

import Project.common.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class TabbedGamePane extends JTabbedPane {
  private GUI gui;
  protected HashMap<Integer, GamePanel> gamePanels = new HashMap<>();

  public TabbedGamePane(GUI gui, HashMap<String, GameBoard> players) {
    this.gui = gui;
    setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 5));
    setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

    int index = 0;
    for(Map.Entry<String, GameBoard> entry : players.entrySet()) {
      removeAll();
      GamePanel tempGamePanel = new GamePanel(entry.getValue());
      gamePanels.put(index, tempGamePanel);
      addTab(entry.getKey(), tempGamePanel);
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
      int selectedIndex = getSelectedIndex() != -1 ? getSelectedIndex() : 0;
      for (int i = 0; i < getTabCount(); i++) {
        JLabel tabLabel = getTabLabel(i);
        if (i == selectedIndex) {
          tabLabel.setForeground(Color.YELLOW);
          setTabComponentAt(i, tabLabel);
        } else {
          tabLabel.setForeground(Color.WHITE);
          setTabComponentAt(i, tabLabel);
        }
      }
    });
  }

  public JLabel getTabLabel(int index) {
    Component component = getTabComponentAt(index);
    if (component instanceof JLabel jLabel) {
      return jLabel;
    } else {
      JLabel newLabel = new JLabel(getTitleAt(index));
      setTabComponentAt(index, newLabel);
      return newLabel;
    }
  }

public void updateGamePanels(HashMap<String, GameBoard> boards) {
  int index = 0;
  for (GameBoard board : boards.values()) {
    if (gamePanels.containsKey(index)) {
      gamePanels.get(index).setGameBoard(board);
      index++;
    } else {
      break;
    }
  }
}

public void ReplaceAll(HashMap<String, GameBoard> boards) {
  removeAll();
  gamePanels.clear();
  int index = 0;
  for(Map.Entry<String, GameBoard> entry : boards.entrySet()) {
    GamePanel tempGamePanel = new GamePanel(entry.getValue());
    gamePanels.put(index, tempGamePanel);
    addTab(entry.getKey(), tempGamePanel);
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
}
}

class GamePanel extends JPanel {
  private GameBoard gameBoard;

  public GamePanel(GameBoard gameBoard) { this.gameBoard = gameBoard; }

  public void setGameBoard(GameBoard gameBoard) {
    this.gameBoard = gameBoard;
    revalidate();
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
  
        PieceType pieceType = gameBoard.getPiece(row, col);

        if (null == pieceType) continue;
        switch (pieceType) {
          case HIT -> {
            g.setColor(Color.RED);
            g.fillRect(x, y, cellSize, cellSize);
          }
          case MISS -> {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, cellSize, cellSize);
          }
          case SHIP -> {
            g.setColor(Color.BLUE);
            g.fillRect(x, y, cellSize, cellSize);
          }
          default -> { }
          }
          g.setColor(Color.WHITE);
      }
    }
  }
}