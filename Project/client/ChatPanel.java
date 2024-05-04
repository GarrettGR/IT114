package Project.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatPanel extends JPanel {
  private JTextArea chatArea;
  private JTextField chatField;
  private JButton sendButton;
  private boolean isConnected;
  private String username;
  private String server;
  private String roomName;
  private JPanel infoPanel;

  public ChatPanel() {
    chatArea = new JTextArea();
    chatField = new JTextField(10);
    sendButton = new JButton("Send");
    isConnected = false;

    chatArea.setEditable(false);

    sendButton.addActionListener((ActionEvent e) -> {
        String message = chatField.getText();
        if (!message.isEmpty()) {
            chatArea.append(message + "\n");
            chatField.setText("");
        }
    });

    infoPanel = new JPanel();
    infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    infoPanel.add(new JLabel("Username:"));
    infoPanel.add(new JLabel(username));
    infoPanel.add(new JLabel("Server:"));
    infoPanel.add(new JLabel(server));
    infoPanel.add(new JLabel("Room:"));
    infoPanel.add(new JLabel(roomName));
  }

  public JPanel getPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    if (!isConnected) {
      ConnectionPanel connectionPanel = new ConnectionPanel();
      panel.add(connectionPanel.getPanel(), BorderLayout.NORTH);
    } else {
      panel.add(infoPanel, BorderLayout.NORTH);
    }

    panel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
    panel.add(chatField, BorderLayout.SOUTH);
    panel.add(sendButton, BorderLayout.EAST);
    return panel;
  }

  public void setConnected(boolean isConnected) { this.isConnected = isConnected; }

  public void setUsername(String username) { this.username = username; }

  public void setServer(String server) { this.server = server; }

  public void setRoomName(String roomName) { this.roomName = roomName; }

  public void appendMessage(String message) { chatArea.append(message + "\n"); }
}

class ConnectionPanel {
  private JTextField usernameField;
  private JTextField hostField;
  private JTextField portField;
  private JButton connectButton;

  public ConnectionPanel() {
    usernameField = new JTextField(10);
    hostField = new JTextField(10);
    portField = new JTextField(5);
    connectButton = new JButton("Connect");
  }

  public JPanel getPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new FlowLayout(FlowLayout.CENTER));
    panel.add(new JLabel("Username:"));
    panel.add(usernameField);
    panel.add(new JLabel("Host:"));
    panel.add(hostField);
    panel.add(new JLabel("Port:"));
    panel.add(portField);
    panel.add(connectButton);
    return panel;
  }
}