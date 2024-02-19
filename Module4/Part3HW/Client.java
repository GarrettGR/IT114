package Module4.Part3HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

    Socket server = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    final String ipAddressPattern = "/connect ([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}:[0-9]{3,5})";
    final String localhostPattern = "/connect (localhost:[0-9]{3,5})";

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";

    boolean isRunning = false;
    private Thread inputThread;
    private Thread fromServerThread;

    public Client() {
        system_print("Client created");
    }

    public static void system_print(String message) {
        System.out.println(ANSI_YELLOW + message + ANSI_RESET);
    }

    public static void system_error(String message) {
        System.err.println(ANSI_RED + message + ANSI_RESET);
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();

    }

    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);

            out = new ObjectOutputStream(server.getOutputStream()); // channel to send to server
            in = new ObjectInputStream(server.getInputStream()); // channel to listen to server

            system_print("Client connected");
            listenForServerMessage();
        } catch (UnknownHostException e) {
            // e.printStackTrace();
            system_error("Unknown host");
        } catch (IOException e) {
            // e.printStackTrace();
            system_error("Error connecting to server");
        }
        return isConnected();
    }

    /**
     * Check if the string contains the <i>connect</i> command
     * followed by an ip address and port or localhost and port.
     * - Example format: /connect 123.123.123:3000
     * - Example format: /connect localhost:3000
     * 
     * @param text
     * @return
     */

    private boolean isConnection(String text) {
        return text.matches(ipAddressPattern) || text.matches(localhostPattern);
    }

    private boolean isQuit(String text) {
        return text.equalsIgnoreCase("quit");
    }

    /**
     * Controller for handling various text commands.
     * 
     * @param text
     * @return true if a text was a command or triggered a command
     */

    private boolean processCommand(String text) {
        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            return true;
        } else if (isQuit(text)) {
            isRunning = false;
            return true;
        }
        return false;
    }

    private void listenForKeyboard() {
        inputThread = new Thread() {
            @Override
            public void run() {
                system_print("Listening for input");
                try (Scanner si = new Scanner(System.in);) {
                    String line = "";
                    isRunning = true;
                    while (isRunning) {
                        try {
                            line = si.nextLine();
                            if (!processCommand(line)) {
                                if (isConnected()) {
                                    out.writeObject(line);

                                } else {
                                    system_print("Not connected to server");
                                }
                            }
                        } catch (Exception e) {
                            system_error("Connection dropped");
                            break;
                        }
                    }
                    system_print("Exited loop");
                } catch (Exception e) {
                    // e.printStackTrace();
                    system_error("Error reading from console");
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
                    String fromServer;
                    while (!server.isClosed() && !server.isInputShutdown()
                            && (fromServer = (String) in.readObject().toString()) != null) {
                        System.out.println(fromServer);
                    }
                    system_print("Loop exited");
                } catch (Exception e) {
                    // e.printStackTrace();
                    if (!server.isClosed()) {
                        system_print("Server closed connection");
                    } else {
                        system_print("Connection closed");
                    }
                } finally {
                    close();
                    system_print("Stopped listening to server input");
                }
            }
        };
        fromServerThread.start();
    }

    public void start() throws IOException {
        listenForKeyboard();
    }

    private void close() {
        try {
            inputThread.interrupt();
        } catch (Exception e) {
            system_error("Error interrupting input");
            // e.printStackTrace();
        }
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            system_error("Error interrupting listener");
            // e.printStackTrace();
        }
        try {
            System.out.println("Closing output stream");
            out.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        } catch (Exception e) {
            system_error("Error closing output stream");
            // e.printStackTrace();
        }
        try {
            System.out.println("Closing input stream");
            in.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        } catch (Exception e) {
            system_error("Error closing input stream");
            // e.printStackTrace();
        }
        try {
            System.out.println("Closing connection");
            server.close();
            System.out.println("Closed socket");
        } catch (IOException e) {
            system_error("Error closing socket");
            // e.printStackTrace();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.start();
        } catch (IOException e) {
            system_error("Error starting client");
            // e.printStackTrace();
        }
    }

}