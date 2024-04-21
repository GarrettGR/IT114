import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Room implements AutoCloseable{
	protected static Server server;
	private String name;
	private List<ServerThread> clients = new ArrayList<ServerThread>();
	private boolean isRunning = false;
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	private final static String DISCONNECT = "disconnect";
	private final static String LOGOUT = "logout";
	private final static String LOGOFF = "logoff";

	public Room(String name) {
		this.name = name;
		isRunning = true;
	}

	private void info(String message) { System.out.println(String.format("Room[%s]: %s", name, message)); }

	public String getName() { return name; }

	protected synchronized void addClient(ServerThread client) {
		if (!isRunning) return;
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			info("Attempting to add a client that already exists");
		} else {
			clients.add(client);
			new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					sendConnectionStatus(client, true);
				}
			}.start();
		}
	}

	protected synchronized void removeClient(ServerThread client) {
		if (!isRunning)
			return;
		clients.remove(client);
		if (clients.size() > 0)
			sendConnectionStatus(client, false);
		checkClients();
	}

	private void checkClients() {
		if (!name.equalsIgnoreCase("lobby") && clients.size() == 0)
			close();
	}

	private boolean processCommands(String message, ServerThread client) {
		boolean wasCommand = false;
		try {
			if (message.startsWith(COMMAND_TRIGGER)) {
				String[] comm = message.split(COMMAND_TRIGGER);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				String roomName;
				wasCommand = true;
				switch (command) {
					case CREATE_ROOM:
						roomName = comm2[1];
						Room.createRoom(roomName, client);
						break;
					case JOIN_ROOM:
						roomName = comm2[1];
						Room.joinRoom(roomName, client);
						break;
					case DISCONNECT:
					case LOGOUT:
					case LOGOFF:
						Room.disconnectClient(client, this);
						break;
					default:
						wasCommand = false;
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wasCommand;
	}

	protected static void createRoom(String roomName, ServerThread client) {
		if (server.createNewRoom(roomName)) {
			Room.joinRoom(roomName, client);
		} else {
			client.sendMessage("Server", String.format("Room %s already exists", roomName));
		}
	}

	protected static void joinRoom(String roomName, ServerThread client) {
		if (!server.joinRoom(roomName, client))
			client.sendMessage("Server", String.format("Room %s doesn't exist", roomName));
	}

	protected static void disconnectClient(ServerThread client, Room room) {
		client.setCurrentRoom(null);
		client.disconnect();
		room.removeClient(client);
	}

	protected synchronized void sendMessage(ServerThread sender, String message) {
		if (!isRunning) return;
		info("Sending message to " + (clients.size() - 1) + " clients");
		if (sender != null && processCommands(message, sender)) return;
		
		String from = (sender == null ? "Room" : sender.getClientName());
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (client == sender) continue;
			boolean messageSent = client.sendMessage(from, message);
			if (!messageSent) handleDisconnect(iter, client);
		}
	}

	protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected){
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			boolean messageSent = client.sendConnectionStatus(sender.getClientName(), isConnected);
			if (!messageSent) handleDisconnect(iter, client);
		}
	}

	private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client){
		iter.remove();
		info("Removed client " + client.getClientName());
		checkClients();
		sendMessage(null, client.getClientName() + " disconnected");
	}

	public void close() {
		server.removeRoom(this);
		server = null;
		isRunning = false;
		clients = null;
	}
}