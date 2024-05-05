package Project.client;

import Project.common.*;
import java.awt.*;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class GUI {
    public GUI(HashMap<String, GameBoard> players, HashMap<String, PlayerData> playerData) {
    JFrame frame = new JFrame("Battleship Game");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 600);
    frame.setLayout(new BorderLayout());
    JPanel userPanelContainer = new UserPanelContainer(playerData);
    
    frame.add(userPanelContainer, BorderLayout.NORTH);

    frame.setVisible(true);
  }

  public static void main(String[] args) {

    String[] playerNames = {"ananya", "abdullah", "will", "garrett"};

    HashMap<String, GameBoard> players = new HashMap<>();
    HashMap<String, PlayerData> playerData = new HashMap<>();

    for (String name : playerNames) {
      players.put(name, new GameBoard());
      playerData.put(name, new PlayerData(10));
    }

    GUI testGUI = new GUI(players, playerData);
  }
}
