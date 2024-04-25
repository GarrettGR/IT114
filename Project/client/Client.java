package Project.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import Project.common.*;

public class Client {

  Socket server = null;
  ObjectOutputStream out = null;
  ObjectInputStream in = null;

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GRAY_BG = "\u001B[48;2;35;35;35m";
  public static final String ANSI_GRAY = "\u001B[38;2;150;150;150m";
  public static final String UNIX_CLEAR = "\033[H\033[2J";
  public static final String SQUARE = "\u25A0";

  public static final String HELP = """
      /connect [ip address]:[port] - Connect to a server
      /connect localhost:[port] - Connect to a server on localhost
      /disconnect - Disconnect from the server
      /name [username] - Set or change username
      /users - List users in the chat
      /pm @[username] ... @[username] [message] - Send a private message
      /clear - Clear the console
      /quit - Exit the program
      /help - Show this help message
      """;

  final String ipAddressPattern = "/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})";
  final String localhostPattern = "/connect\\s+(localhost:\\d{3,5})";
  boolean isRunning = false;
  private Thread inputThread;
  private Thread fromServerThread;
  private String clientName = "";

  public Client() { system_print("Client Created"); }

  public boolean isConnected() {
    if (server == null)
      return false;
    return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
  }

  private boolean connect(String address, int port) {
    try {
      server = new Socket(address, port);
      out = new ObjectOutputStream(server.getOutputStream());
      in = new ObjectInputStream(server.getInputStream());
      system_print("Client connected");
      listenForServerMessage();
      sendConnect();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return isConnected();
  }

  private boolean isConnection(String text) { return text.matches(ipAddressPattern) || text.matches(localhostPattern); }

  private boolean isName(String text) {
    if (text.startsWith("/name")) {
      String[] parts = text.split(" ");
      if (parts.length >= 2) {
        clientName = parts[1].trim();
        system_print("Name set to " + clientName);
      }
      return true;
    }
    return false;
  }

  private boolean isQuit(String text) { return text.equalsIgnoreCase("/quit"); }

  private boolean isHelp(String text) { return text.equalsIgnoreCase("/help"); }

  private boolean isClear(String text) { return text.equalsIgnoreCase("/clear"); }

  public static void server_print(String message) {
    if (message.startsWith("Server:")) {
      System.out.println(ANSI_GRAY_BG + ANSI_YELLOW + message + ANSI_RESET);
    } else {
      System.out.println(ANSI_GRAY_BG + message + ANSI_RESET);
    }
  }

  public static void system_print(String message) { System.out.println(ANSI_YELLOW + message + ANSI_RESET); }

  public static void system_error(String message) { System.out.println(ANSI_RED + message + ANSI_RESET); }

  public static void game_print(String message) { System.out.print(ANSI_GRAY + message + ANSI_RESET);}

  private boolean processCommand(String text) {
    if (isConnection(text)) {
      if (clientName.isBlank())
        system_print("You can set your name by using: /name your_name");
      String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
      connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
      return true;
    } else if (isQuit(text)) {
      isRunning = false;
      return true;
    } else if (isName(text) && !isConnected()) {
      return true;
    } else if (isHelp(text)) {
      system_print(HELP);
      return true;
    } else if (isClear(text)) {
      System.out.print(UNIX_CLEAR);
      System.out.flush();
      system_print("Console cleared");
      return true;
    }
    return false;
  }

  private void sendConnect() throws IOException {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.CONNECT);
    p.setClientName(clientName);
    out.writeObject(p);
  }

  private void sendMessage(String message) throws IOException {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.MESSAGE);
    p.setMessage(message);
    p.setClientName(clientName);
    out.writeObject(p);
  }

  private void listenForKeyboard() {
    inputThread = new Thread() {
      @Override
      public void run() {
        system_print("Enter /help to see a list of commands");
        try (Scanner si = new Scanner(System.in);) {
          String line = "";
          isRunning = true;
          while (isRunning) {
            try {
              line = si.nextLine();
              if (!processCommand(line)) {
                if (isConnected()) {
                  if (line != null && line.trim().length() > 0) {
                    sendMessage(line);
                  }
                } else {
                  system_error("Not connected to server");
                }
              }
            } catch (Exception e) {
              system_error("Connection dropped");
              break;
            }
          }
          system_error("Exited loop");
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          close();
        }
      }
    };
    inputThread.start();
  }

  private void listenForServerMessage() {
    fromServerThread = new Thread() {
      @Override
      public void run() {
        try {
          Payload fromServer;

          while (!server.isClosed() && !server.isInputShutdown() && (fromServer = (Payload) in.readObject()) != null)
            processMessage(fromServer);
          system_error("Loop exited");
        } catch (Exception e) {
          e.printStackTrace();
          if (!server.isClosed()) {
            system_error("Server closed connection");
          } else {
            system_error("Connection closed");
          }
        } finally {
          close();
          system_print("Stopped listening to server input");
        }
      }
    };
    fromServerThread.start();
  }

  private void processMessage(Payload p) {
    switch (p.getPayloadType()) {
      case CONNECT:
      case DISCONNECT:
        system_print(String.format("*%s %s*",
            p.getClientName(),
            p.getMessage()));
        break;
      case MESSAGE:
        server_print(String.format("%s: %s",
            p.getClientName(),
            p.getMessage()));
        break;
      case GAME_START:
        system_print("Game starting");
        drawGame(p.getPlayerBoard(), p.getOpponentBoard());
        break;
      case PING:
      default:
        break;
    }
  }

  private void drawBoard(PieceType[][] board) {
    final char EMPTY_SPACE = ' ';

    for (PieceType[] row : board) {
      System.out.print(' ');
      for (PieceType piece : row) {
        game_print("[");
        switch (piece) {
          case EMPTY:
            System.out.print(EMPTY_SPACE + "");
            break;
          case SHIP:
            System.out.print(SQUARE);
            break;
          case HIT:
            System.out.print(ANSI_RED + SQUARE + ANSI_RESET);
            break;
          case MISS:
            game_print("X");
            break;
          default:
            break;
        }
        game_print("]");
      }
      System.out.println();
    }
  }

  private void drawGame(PieceType[][] OpponentBoard, PieceType[][] PlayerBoard) {
    drawBoard(OpponentBoard);
    System.out.print(' ');
    for (int i = 0; i < 30; i++) game_print("-");
    System.out.println();
    drawBoard(PlayerBoard);
  }

  public void start() throws IOException { listenForKeyboard(); }

  private void close() {
    try {
      inputThread.interrupt();
    } catch (Exception e) {
      system_error("Error interrupting input");
      e.printStackTrace();
    }
    try {
      fromServerThread.interrupt();
    } catch (Exception e) {
      system_error("Error interrupting listener");
      e.printStackTrace();
    }
    try {
      system_print("Closing output stream");
      out.close();
    } catch (NullPointerException ne) {
      system_print("Server was never opened so this exception is ok");
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      system_print("Closing input stream");
      in.close();
    } catch (NullPointerException ne) {
      system_print("Server was never opened so this exception is ok");
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      system_print("Closing connection");
      server.close();
      system_print("Closed socket");
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NullPointerException ne) {
      system_print("Server was never opened so this exception is ok");
    }
  }

  public static void main(String[] args) {
    Client client = new Client();
    try {
      client.start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
