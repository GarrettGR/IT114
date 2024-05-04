package Project.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class UserPanel extends JPanel {
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

    infoPanel.setLayout(new GridLayout(6, 1));
    infoPanel.add(hitsLabel);
    infoPanel.add(missesLabel);
    infoPanel.add(accuracyLabel);
    infoPanel.add(pointsLabel);
    infoPanel.add(currencyLabel);
    JPanel namePanel = new JPanel();

    namePanel.setLayout(new BorderLayout());
    namePanel.add(nameLabel, BorderLayout.CENTER);
    namePanel.add(statusPanel, BorderLayout.NORTH);
    namePanel.add(healthLabel, BorderLayout.SOUTH);
    namePanel.add(infoPanel, BorderLayout.SOUTH);
    add(namePanel, BorderLayout.WEST);

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

  public void updateHealth(int health) { healthLabel.setText("Health: " + health); }

  public void updatePoints(int points) { pointsLabel.setText("Points: " + points); }

  public void updateCurrency(int currency) { currencyLabel.setText("Currency: " + currency); }

  private void updateHits(int hits) { hitsLabel.setText("Hits: " + hits); }

  private void updateMisses(int misses) { missesLabel.setText("Misses: " + misses); }

  public void updateAccuracy(int hits, int misses) {
    this.updateHits(hits);
    this.updateMisses(misses);
    int totalShots = hits + misses;
    double accuracy = (totalShots > 0) ? (double) hits / totalShots * 100 : 0;
    accuracyLabel.setText("Accuracy: " + String.format("%.2f", accuracy) + "%");
  }

  public void toggleInfoPanel() {
    infoPanelVisible = !infoPanelVisible;
    infoPanel.setVisible(infoPanelVisible);
  }
}