package Project.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// Yes... I know making these static is lazy, but i don't have the energy to do it right (atm)
public class ChatScrollPane extends JScrollPane {
  private GUI gui;
  private JTextArea chatArea;
  private JTextField chatField;
  private JButton sendButton;
  private boolean isConnected = false;
  protected String username;
  private String server;
  private String roomName;
  private JPanel infoPanel;
  private JPanel chatPanel;
  private JLabel usernameLabel = new JLabel(); 
  private JLabel serverLabel = new JLabel();
  // private JLabel roomNameLabel = new JLabel();


  public ChatScrollPane(GUI gui) {
    this.gui = gui;
    chatArea = new JTextArea();
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);
    chatField = new JTextField(15);
    sendButton = new JButton("Send");
    chatArea.setEditable(false);

    infoPanel = new JPanel();
    infoPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    infoPanel.add(usernameLabel);
    infoPanel.add(serverLabel);
    // infoPanel.add(roomNameLabel);

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

  protected void pushMessage(String message) { gui.pushMessage(message); }

  private void sendMessage() {
    String message = chatField.getText();
    if (!message.isEmpty()) {
      printMessage(username + ": " + message);
      pushMessage(message);
    }
  }

  protected void printMessage(String message) {
    chatArea.append(message + "\n");
    chatArea.setCaretPosition(chatArea.getDocument().getLength());
    chatField.setText("");  
  }

  public JPanel getPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    if (!isConnected) {
      panel.add(new ConnectionPanel(this), BorderLayout.NORTH);
    } else {
      panel.add(infoPanel, BorderLayout.NORTH);
      panel.add(this, BorderLayout.CENTER);
      panel.add(chatPanel, BorderLayout.SOUTH);
    }
    return panel;
  }

  public void updateConnection(boolean isConnected) { this.isConnected = isConnected; }

  public void sendConnection(String username, String server, int port) {
    pushMessage(String.format("/name %s", username));
    pushMessage(String.format("/connect %s:%s", server, port));
    this.username = username;
    usernameLabel.setText("Username: " + username);
    serverLabel.setText("Server: " + server);
  }

  public void setUsername(String username) { this.username = username; }

  public String getUsername() { return username; }

  public void setServer(String server) { this.server = server; }

  public void setRoomName(String roomName) { this.roomName = roomName; }

  public void clearChat() { chatArea.setText(""); }

  public void clearField() { chatField.setText(""); }

  public String getMessage() { return chatField.getText(); }
}

class ConnectionPanel extends JPanel {
  private ChatScrollPane chat;
  private JTextField usernameField;
  private JTextField hostField;
  private JTextField portField;
  private JButton connectButton;

  public ConnectionPanel(ChatScrollPane chatScrollPane) {
    this.chat = chatScrollPane;
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
      chat.sendConnection(usernameField.getText(), hostField.getText(), Integer.parseInt(portField.getText()));
    });
  }
}
