package Project.client;

import Project.common.PlayerData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class UserPanelContainer extends JPanel {
  ArrayList<UserPanel> userPanels = new ArrayList<>();

  public UserPanelContainer(HashMap<String, PlayerData> playerData) {
    
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    add(Box.createHorizontalGlue());  

    for (Map.Entry<String, PlayerData> entry : playerData.entrySet()) {
      UserPanel tempUserPanel = new UserPanel(entry.getKey());
      tempUserPanel.setHealth(entry.getValue().getHealth());
      tempUserPanel.setPoints(entry.getValue().getScore());
      tempUserPanel.setCurrency(entry.getValue().getCurrency());
      tempUserPanel.setAccuracy(entry.getValue().getHits(), entry.getValue().getMisses());
      userPanels.add(tempUserPanel);
      add(tempUserPanel);
    }
  }

}

class UserPanel extends JPanel {
  private JLabel nameLabel;
  private JLabel healthLabel;
  private JPanel statusPanel;
  private JLabel hitsLabel;
  private JLabel missesLabel;
  private JLabel accuracyLabel;
  private JLabel pointsLabel;
  private JLabel currencyLabel;
  private JPanel infoPanel;
  private boolean infoPanelVisible;

  public UserPanel(String name) {
    setLayout(new BorderLayout());
    add(Box.createVerticalGlue());

    nameLabel = new JLabel(name);
    healthLabel = new JLabel("Health: 0");
    statusPanel = new JPanel();
    this.setStatus(false, false);
    hitsLabel = new JLabel("Hits: 0");
    missesLabel = new JLabel("Misses: 0");
    accuracyLabel = new JLabel("Accuracy: 0%");
    pointsLabel = new JLabel("Points: 0");
    currencyLabel = new JLabel("Currency: 0");
    infoPanel = new JPanel();

    infoPanel.setLayout(new GridLayout(5, 1));
    infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
    infoPanel.add(hitsLabel);
    infoPanel.add(missesLabel);
    infoPanel.add(accuracyLabel);
    infoPanel.add(pointsLabel);
    infoPanel.add(currencyLabel);

    JPanel namePanel = new JPanel();

    namePanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = new Insets(0, 0,  2, 5);

    // Status Panel
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 2;
    gbc.weightx = 0; 
    gbc.weighty = 0;
    gbc.ipadx = 10;
    namePanel.add(statusPanel, gbc);

    // Name Label
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridheight = 1; 
    gbc.weightx = 1;
    gbc.weighty = 0; 
    namePanel.add(nameLabel, gbc);

    // Health Label
    gbc.gridx = 1;
    gbc.gridy = 1;
    namePanel.add(healthLabel, gbc);

    add(namePanel, BorderLayout.NORTH);
    add(infoPanel, BorderLayout.SOUTH);

    nameLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        toggleInfoPanel();
      }
    });
    
    infoPanelVisible = false;
    infoPanel.setVisible(infoPanelVisible);
  }

  public final void setStatus(boolean isTurn, boolean isAway) {
    if (isAway) statusPanel.setBackground(Color.RED);
    else if (!isTurn) statusPanel.setBackground(Color.GRAY);
    else statusPanel.setBackground(Color.GREEN);
  }

  public void setHealth(int health) { healthLabel.setText("Health: " + health); }

  public void setPoints(int points) { pointsLabel.setText("Points: " + points); }

  public void setCurrency(int currency) { currencyLabel.setText("Currency: " + currency); }

  private void setHits(int hits) { hitsLabel.setText("Hits: " + hits); }

  private void setMisses(int misses) { missesLabel.setText("Misses: " + misses); }

  public void setAccuracy(int hits, int misses) {
    this.setHits(hits);
    this.setMisses(misses);
    int totalShots = hits + misses;
    double accuracy = (totalShots > 0) ? (double) hits / totalShots * 100 : 0;
    accuracyLabel.setText("Accuracy: " + String.format("%.2f", accuracy) + "%");
  }

  public void toggleInfoPanel() {
    infoPanelVisible = !infoPanelVisible;
    infoPanel.setVisible(infoPanelVisible);
  }
}