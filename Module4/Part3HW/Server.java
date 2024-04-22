package Module4.Part3HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
    int port = 3000;

    private List<ServerThread> clients = new ArrayList<ServerThread>();
    private int clientNum = 0;

    private void start(int port) {
        this.port = port;
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            Socket incoming_client = null;
            System.out.println("Server is listening on port " + port);
            do {
                System.out.println("waiting for next client");
                if (incoming_client != null) {
                    System.out.println("Client connected");
                    ServerThread sClient = new ServerThread(incoming_client, this, "User" + clientNum++);
                    clients.add(sClient);
                    sClient.start();
                    incoming_client = null;
                }
            } while ((incoming_client = serverSocket.accept()) != null);
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    protected synchronized void disconnect(ServerThread client) {
        String userName = client.getUserName();
        client.disconnect();
        broadcast("Disconnected", userName);
    }

    protected synchronized void broadcast(String message, String userName) {

        if (processCommand(message, userName)) {
            return;
        }

        if (!message.startsWith("Server:")) {
            message = String.format("%s: %s", userName, message);
        }

        Iterator<ServerThread> it = clients.iterator();
        while (it.hasNext()) {
            ServerThread client = it.next();
            if (client.getUserName() == userName) {
                continue;
            }
            boolean wasSuccessful = client.send(message);
            if (!wasSuccessful) {
                System.out.println(String.format("Removing disconnected client[%s] from list", client.getUserName()));
                it.remove();
                broadcast("Disconnected", userName);
            }
        }
    }

    private ServerThread getClient(String userName) {
        for (ServerThread client : clients) {
            if (client.getUserName().equals(userName)) {
                return client;
            }
        }
        return null;
    }

    private boolean processCommand(String message, String userName) {
        ServerThread client = getClient(userName);

        if (message.equalsIgnoreCase("/disconnect")) {
            clients.remove(client);
            disconnect(client);
            return true;
        } else if (message.equalsIgnoreCase("/users")) {
            String [list] = "Server: ";
            for (ServerThread clnt : clients) {
                list += clnt.getUserName() + ", ";
            }
            list = list.substring(0, list.length() - 2);
            client.send(list);
            return true;
        } else if (message.toLowerCase().startsWith("/rename ")) {

            String newName = message.split(" +")[1];

            String unavailable[] = new String[clients.size() + 4];
            clients.forEach(clnt -> unavailable[clients.indexOf(clnt)] = clnt.getUserName());
            unavailable[clients.size()] = "Server";
            unavailable[clients.size() + 1] = "User" + clientNum;
            unavailable[clients.size() + 2] = "Thread";
            unavailable[clients.size() + 3] = "Client";

            for (String name : unavailable) {
                if (newName.equalsIgnoreCase(name)) {
                    client.send(String.format("%s: %s", "Server", "Name already in use"));
                    return true;
                }
            }

            client.setUserName(newName);
            client.send(String.format("%s: %s", "Server", "Renamed to " + newName));
            broadcast(String.format("Server: %s renamed to %s", userName, newName), newName);
            return true;
        } else if (message.toLowerCase().startsWith("/pm ")) {
            // grg -- 2-19-24
            String parts[] = message.substring(4).split(" +");
            List<String> targets = new ArrayList<String>();
            StringBuilder privMsg = new StringBuilder();

            for (String part : parts) {
                if (part.startsWith("@")) {
                    targets.add(part.substring(1));
                } else if (!part.startsWith("@")) {
                    privMsg.append(part + " ");
                }
            }

            if (privMsg.length() == 0) {
                client.send("Server: No message provided");
                return true;
            } else if (parts.length < 2) {
                client.send("Server: The command was not formatted correctly");
                return true;
            } else if (targets.isEmpty()) {
                client.send("Server: No targets provided");
                return true;
            }

            for (String target : targets) {
                ServerThread clnt = getClient(target);

                if (clnt != null) {
                    if (clnt.getUserName().equals(userName)) {
                        client.send("Server: You cannot send a private message to yourself");
                        continue;
                    } else if (clnt.getUserName().equals("Server")) {
                        client.send("Server: You cannot send a private message to the server");
                        continue;
                    } else if (targets.size() > 1) {
                        String target_users = "";
                        for (String target_user : targets) {
                            target_users += target_user + ", ";
                        }
                        target_users = target_users += userName;
                        clnt.send(String.format("\nPrivate message with %s\n%s: %s", target_users, userName, privMsg));
                    } else if (targets.size() == 1) {
                        clnt.send(String.format("Private message from %s: %s", userName, privMsg));
                    }

                } else {
                    client.send(String.format("Server: %s is not connected", target));
                }
            }

            return true;
        } else if (message.toLowerCase().startsWith("/shuffle ")) {
            // grg -- 2-19-24
            String input = message.substring(9);
            List<Character> chars = new ArrayList<Character>();
            StringBuilder output = new StringBuilder(input.length());

            if (input.length() < 2) {
                client.send("Server: Not enough characters to shuffle");
                return true;
            }

            for (char c : input.toCharArray()) {
                chars.add(c);
            }
            while (chars.size() != 0) {
                int rand = (int) (Math.random() * chars.size());
                output.append(chars.remove(rand));
            }

            client.send("Server: your shuffled message: " + output.toString());
            broadcast(output.toString(), userName);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println("Starting Server");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println("No port provided. Using default port: " + port);
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}