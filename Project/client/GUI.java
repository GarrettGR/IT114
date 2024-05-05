package Project.client;

import Project.common.GameBoard;
import java.awt.*;
import java.util.HashMap;
import javax.swing.*;


public class GUI {
    public GUI(HashMap<String, GameBoard> players) {
    JFrame frame = new JFrame("Battleship Game");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 600);
    frame.setLayout(new BorderLayout());

    JPanel userPanels = new JPanel();
    userPanels.setLayout(new BoxLayout(userPanels, BoxLayout.X_AXIS));

    JPanel gamePanels = new JPanel();
    gamePanels.setLayout(new BoxLayout(gamePanels, BoxLayout.X_AXIS));

    // Bring everything together

    ChatPanel chatPanel = new ChatPanel();

    frame.add(userPanels, BorderLayout.NORTH);
    frame.add(gamePanels, BorderLayout.CENTER);
    frame.add(chatPanel, BorderLayout.EAST);

    frame.setVisible(true);
  }
}
