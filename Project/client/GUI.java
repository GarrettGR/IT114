package Project.client;

import Project.common.*;
import java.awt.*;
import javax.swing.*;
import java.util.HashMap;

public class GUI {
  private JFrame frame;
  private static Client client;
  private UserPanelContainer userPanelContainer;
  private TabbedGamePane tabbedGamePane;
  private ChatScrollPane chatScrollPane;
  private boolean gameDrawn = false;

  public GUI(Client client) {
    this.client = client;
    frame = new JFrame("Battleship Game");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 665);
    frame.setLayout(new BorderLayout());

    ChatScrollPane chatScrollPane = new ChatScrollPane();
    
    frame.add(chatScrollPane.getPanel(), BorderLayout.EAST);

    frame.setVisible(true);
  }

  public void ingestMessage(String message) { ChatScrollPane.printMessage(message); }

  public static void pushMessage(String message) { 
    try { 
      client.handleMessage(message); 
    } catch (Exception e) { 
      ChatScrollPane.printMessage("Error: " + e.getMessage()); 
    }
  }

  public void ingestData(HashMap<String, PlayerData> playerData, HashMap<String, GameBoard> players) {
    if (!gameDrawn) {
      userPanelContainer = new UserPanelContainer(playerData);
      tabbedGamePane = new TabbedGamePane(players);
      drawGame();
      gameDrawn = true;
    } else {
      userPanelContainer.updateUserPanels(playerData);
      tabbedGamePane.updateGamePanels(players);
    }
  }

  public void drawGame(){
    frame.add(userPanelContainer, BorderLayout.NORTH);
    frame.add(tabbedGamePane, BorderLayout.CENTER);
    frame.add(chatScrollPane.getPanel(), BorderLayout.EAST);
  }

  public void setClient(Client client) { this.client = client; }
}
