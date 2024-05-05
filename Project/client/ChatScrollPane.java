package Project.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ChatScrollPane extends JScrollPane {
  private static JTextArea chatArea;
  private static JTextField chatField;
  private JButton sendButton;
  private boolean isConnected = true;
  private String username;
  private String server;
  private String roomName;
  private JPanel infoPanel;
  private JPanel chatPanel;

  public ChatScrollPane() {
    chatArea = new JTextArea();
    chatField = new JTextField(15);
    sendButton = new JButton("Send");
    chatArea.setEditable(false);

    infoPanel = new JPanel();
    infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    infoPanel.add(new JLabel("Username:"));
    infoPanel.add(new JLabel(username));
    infoPanel.add(new JLabel("Server:"));
    infoPanel.add(new JLabel(server));
    infoPanel.add(new JLabel("Room:"));
    infoPanel.add(new JLabel(roomName));

    chatPanel = new JPanel();
    chatPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
    chatPanel.add(chatField);
    chatPanel.add(sendButton);

    setViewportView(chatArea);
    setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    sendButton.addActionListener((ActionEvent e) -> { sendMessage(); });

    chatField.addActionListener((ActionEvent e) -> { sendMessage(); });
  }

  protected static void pushMessage(String message) {
    System.out.println(message);
  }

  private void sendMessage() {
    String message = chatField.getText();
    if (!message.isEmpty()) {
      pushMessage(message);
      printMessage(username + ": " + message);
    }
  }

  protected static void printMessage(String message) {
    chatArea.append(message + "\n");
    chatField.setText("");  
  }

  public JPanel getPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    if (!isConnected) {
      panel.add(new ConnectionPanel(), BorderLayout.NORTH);
    } else {
      panel.add(infoPanel, BorderLayout.NORTH);
      panel.add(chatArea, BorderLayout.CENTER);
      panel.add(chatPanel, BorderLayout.SOUTH);
    }
    return panel;
  }

  public void setConnected(boolean isConnected) { this.isConnected = isConnected; }

  public static void sendConnection(String username, String server, int port) {
    System.out.println(String.format("%s@%s:%s", username, server, port));
    pushMessage(String.format("/connect %s:%s", server, port));
  }

  public void setUsername(String username) { this.username = username; }

  public void setServer(String server) { this.server = server; }

  public void setRoomName(String roomName) { this.roomName = roomName; }

  public void clearChat() { chatArea.setText(""); }

  public void clearField() { chatField.setText(""); }

  public String getMessage() { return chatField.getText(); }
}

class ConnectionPanel extends JPanel {
  private JTextField usernameField;
  private JTextField hostField;
  private JTextField portField;
  private JButton connectButton;

  public ConnectionPanel() {
    usernameField = new JTextField(10);
    hostField = new JTextField(15);
    portField = new JTextField(5);
    connectButton = new JButton("Connect");

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBorder(BorderFactory.createEmptyBorder(35, 5, 0, 10));

    add(new JLabel("Username:"));
    add(Box.createVerticalStrut(5));
    add(usernameField);

    add(Box.createVerticalStrut(10));
    
    add(new JLabel("Host:"));
    add(Box.createVerticalStrut(5));
    add(hostField);
    
    add(Box.createVerticalStrut(10));
    
    add(new JLabel("Port:"));
    add(Box.createVerticalStrut(5));
    add(portField);

    add(Box.createVerticalStrut(10));
    
    add(connectButton);

    connectButton.addActionListener((ActionEvent e) -> {
      ChatScrollPane.sendConnection(usernameField.getText(), hostField.getText(), Integer.parseInt(portField.getText()));
    });
  }
}
