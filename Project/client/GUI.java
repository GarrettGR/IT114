package Project.client;

import Project.common.*;
import java.awt.*;
import java.util.HashMap;
import javax.swing.*;

public class GUI {
  private JFrame frame;
  private Client client;
  private UserPanelContainer userPanelContainer;
  private TabbedGamePane tabbedGamePane;
  private ChatScrollPane chatScrollPane;
  private int drawCount = 0;

  public GUI(Client client) {
    this.client = client;
    frame = new JFrame("Battleship Game");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 665);
    frame.setLayout(new BorderLayout());

    chatScrollPane = new ChatScrollPane(this);
    
    frame.add(chatScrollPane.getPanel(), BorderLayout.EAST);

    frame.setVisible(true);
  }

  public void ingestMessage(String message) { chatScrollPane.printMessage(message); }

  public void pushMessage(String message) { 
    try { 
      System.out.println(message);
      client.handleMessage(message);
    } catch (Exception e) { 
      chatScrollPane.printMessage("Error: " + e.getMessage()); 
    }
  }

  public synchronized void ingestData(HashMap<String, PlayerData> playerData, HashMap<String, GameBoard> players) {
    HashMap<String, PlayerData> playerDataCopy = new HashMap<>(playerData);
    HashMap<String, GameBoard> playersCopy = new HashMap<>(players);
    if (drawCount == 0){
      userPanelContainer = new UserPanelContainer(this, playerData);
      tabbedGamePane = new TabbedGamePane(this, players);
      drawCount++;
    } else {
      // if (drawCount == 1){
      //   userPanelContainer.replaceAll(playerDataCopy); //? Just use the "new" keyword and replace it ??
      //   tabbedGamePane.ReplaceAll(playersCopy);
      //   drawCount++;
      // } else {
      //   userPanelContainer.updateUserPanels(playerDataCopy);
      //   tabbedGamePane.updateGamePanels(playersCopy);
      // }
      userPanelContainer.replaceAll(playerDataCopy);
      tabbedGamePane.ReplaceAll(playersCopy);
    }

    frame.getContentPane().removeAll();
    frame.add(userPanelContainer, BorderLayout.NORTH);
    frame.add(tabbedGamePane, BorderLayout.CENTER);
    frame.add(chatScrollPane.getPanel(), BorderLayout.EAST);

    frame.revalidate();
    frame.repaint();
  }

  public void updateConnection (boolean isConnected) { 
    frame.getContentPane().removeAll();
    chatScrollPane = new ChatScrollPane(this);
    chatScrollPane.updateConnection(isConnected); 
    frame.add(chatScrollPane.getPanel(), BorderLayout.EAST);
    frame.revalidate();
    frame.repaint();  
  }

  public String getUserName() { return chatScrollPane.getUsername(); }

  public void updateName() { chatScrollPane.setUsername(client.getClientName()); }

  public void setClient(Client client) { this.client = client; }
}
