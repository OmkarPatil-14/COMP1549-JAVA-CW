import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

// Singleton Pattern
class ServerSingleton {
    private static ServerSingleton instance;
    private Server server;

    private ServerSingleton() {
        this.server = new Server();
    }

    public static ServerSingleton getInstance() {
        if (instance == null) {
            instance = new ServerSingleton();
        }
        return instance;
    }

    public Server getServer() {
        return server;
    }
}

// Server class
class Server {
    public static final int SERVER_PORT = 6000;
    private static Map<String, ClientHandler> clients = new HashMap<>();
    public static ClientHandler coordinator;
    static InetAddress serverIP;

    public void start() {
        try {
            serverIP = InetAddress.getLocalHost();
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on " + serverIP.getHostAddress() + ":" + SERVER_PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = ClientHandlerFactory.createClientHandler(socket);
                String clientId = clientHandler.getClientId();

                if (clients.containsKey(clientId)) {
                    String message = getCurrentTimestamp() + ": Client ID already taken. Disconnecting client.";
                    System.out.println(message);
                    clientHandler.sendMessage("Server", message);
                    socket.close();
                    continue;
                }

                clients.put(clientId, clientHandler);

                if (coordinator == null) {
                    coordinator = clientHandler;
                    coordinator.setCoordinator(true);
                    String message = getCurrentTimestamp() + ": New coordinator: " + coordinator.getClientId();
                    System.out.println(message);
                    broadcastMessage("Server", message);
                } else {
                    clientHandler.setCoordinator(coordinator);
                    String message = getCurrentTimestamp() + ": New client connected: " + clientId + ".";
                    clientHandler.sendMessage("Server", message + "\nCurrent coordinator is " + coordinator.getClientId());
                }

                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcastMessage(String senderId, String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(senderId, message);
        }
    }

// Fault Tolerance
    static void removeClient(ClientHandler client) {
        String clientId = client.getClientId();
        clients.remove(clientId);
        String message = getCurrentTimestamp() + ": Client " + clientId + " disconnected.";
        System.out.println(message);
        broadcastMessage("Server", message);

        if (client == coordinator) {
            if (!clients.isEmpty()) {
                coordinator = clients.values().iterator().next();
                coordinator.setCoordinator(true);
                message = getCurrentTimestamp() + ": New coordinator: " + coordinator.getClientId();
                System.out.println(message);
                broadcastMessage("Server", message);
            } else {
                coordinator = null;
            }
        }
    }

// Fault Tolerance
    static void removeClientAbruptly(ClientHandler client) {
        String clientId = client.getClientId();
        clients.remove(clientId);
        String message = getCurrentTimestamp() + ": Client " + clientId + " disconnected abruptly.";
        System.out.println(message);
        broadcastMessage("Server", message);

        if (client == coordinator) {
            if (!clients.isEmpty()) {
                coordinator = clients.values().iterator().next();
                coordinator.setCoordinator(true);
                message = getCurrentTimestamp() + ": New coordinator: " + coordinator.getClientId();
                System.out.println(message);
                broadcastMessage("Server", message);
            } else {
                coordinator = null;
            }
        }
    }

    static Map<String, ClientHandler> getClients() {
        return clients;
    }

    static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }
}

// Factory Pattern
class ClientHandlerFactory {
    public static ClientHandler createClientHandler(Socket socket) {
        return new ClientHandler(socket);
    }
}

// ClientHandler class
class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    public String clientId;
    public ClientHandler coordinator;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            clientId = reader.readLine(); // Read the client ID from the client
            System.out.println(Server.getCurrentTimestamp() + ": New client connected: " + clientId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("/quit")) {
                    Server.removeClient(this);
                    sendMessage("Server", Server.getCurrentTimestamp() + ": You left the chat.");
                    break;
                } else if (message.startsWith("@")) {
                    String[] parts = message.split(" ", 2);
                    String targetId = parts[0].substring(1);
                    String content = parts[1];

                    if (content.equals("/memberdetails") && targetId.equals(coordinator.getClientId())) {
                        sendMemberDetails();
                    } else {
                        sendPrivateMessage(targetId, content);
                    }
                } else {
                    String timestamp = Server.getCurrentTimestamp();
                    System.out.println(timestamp + ": " + clientId + ": " + message);
                    Server.broadcastMessage(clientId, timestamp + ": " + message);
                }
            }
        } catch (IOException e) {
            Server.removeClientAbruptly(this);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void sendMessage(String senderId, String message) {
        writer.println(senderId + ": " + message);
    }

    void sendPrivateMessage(String targetId, String message) {
        ClientHandler targetClient = Server.getClients().get(targetId);
        if (targetClient != null) {
            targetClient.sendMessage(clientId, "Private: " + message);
            System.out.println(Server.getCurrentTimestamp() + ": " + clientId + " -> " + targetId + ": " + message);
        } else {
            sendMessage("Server", Server.getCurrentTimestamp() + ": Client with ID " + targetId + " not found.");
        }
    }

    void sendMemberDetails() {
        StringBuilder memberDetails = new StringBuilder();
        memberDetails.append(Server.getCurrentTimestamp() + ": Member Details:\n");
        for (Map.Entry<String, ClientHandler> entry : Server.getClients().entrySet()) {
            memberDetails.append("Client ID: ").append(entry.getKey()).append("\n");
            memberDetails.append("IP Address: ").append(entry.getValue().getSocket().getInetAddress().getHostAddress()).append("\n");
            memberDetails.append("Port: ").append(entry.getValue().getSocket().getPort()).append("\n");
        }
        memberDetails.append("Coordinator: ").append(coordinator.getClientId());
        sendMessage("Coordinator", memberDetails.toString());
    }

    public static void main(String[] args) {
        ServerSingleton.getInstance().getServer().start();
    }
}

