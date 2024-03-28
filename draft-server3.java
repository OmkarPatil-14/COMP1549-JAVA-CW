import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

// Server class
class Server {
    private static final int SERVER_PORT = 8000;
    private static Map<String, ClientHandler> clients = new HashMap<>();
    private static ClientHandler coordinator;
    private static InetAddress serverIP;

    public static void main(String[] args) {
        try {
            serverIP = InetAddress.getLocalHost();
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on " + serverIP.getHostAddress() + ":" + SERVER_PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
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
                    String message = getCurrentTimestamp() + ": New client connected: " + clientId + ". Coordinator: " + coordinator.getClientId();
                    broadcastMessage("Server", message);
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

    static void removeClient(ClientHandler client) {
        String clientId = client.getClientId();
        clients.remove(clientId);
        String message = getCurrentTimestamp() + ": Client " + clientId + " disconnected.";
        System.out.println(message);
        broadcastMessage("Server", message);
        client.sendMessage("Server", getCurrentTimestamp() + ": You left the chat.");

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

// ClientHandler class
class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String clientId;
    private ClientHandler coordinator;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            clientId = "omkar1344";
            System.out.println(Server.getCurrentTimestamp() + ": New client connected: " + clientId);
            sendMessage("Server", Server.getCurrentTimestamp() + ": Your unique ID is " + clientId);
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
                    break;
                } else if (message.startsWith("@")) {
                    String[] parts = message.split(" ", 2);
                    String targetId = parts[0].substring(1);
                    String command = parts[1];
                    if (command.equals("/memberdetails") && targetId.equals(coordinator.getClientId())) {
                        coordinator.sendMemberDetails(this);
                    } else if (command.startsWith("@")) {
                        String[] privateParts = command.split(" ", 2);
                        String privateTargetId = privateParts[0].substring(1);
                        String privateMessage = privateParts[1];
                        sendPrivateMessage(privateTargetId, privateMessage);
                    } else {
                        sendMessage("Server", Server.getCurrentTimestamp() + ": Invalid command");
                    }
                } else {
                    String timestamp = Server.getCurrentTimestamp();
                    System.out.println(timestamp + ": " + clientId + ": " + message);
                    Server.broadcastMessage(clientId, timestamp + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    void sendMemberDetails(ClientHandler client) {
        StringBuilder memberDetails = new StringBuilder();
        memberDetails.append(Server.getCurrentTimestamp() + ": Member Details:\n");
        for (Map.Entry<String, ClientHandler> entry : Server.getClients().entrySet()) {
            memberDetails.append("Client ID: ").append(entry.getKey()).append("\n");
            memberDetails.append("IP Address: ").append(entry.getValue().getSocket().getInetAddress().getHostAddress()).append("\n");
            memberDetails.append("Port: ").append(entry.getValue().getSocket().getPort()).append("\n");
        }
        memberDetails.append("Coordinator: ").append(coordinator.getClientId());
        client.sendMessage("Coordinator", memberDetails.toString());
    }

    void setCoordinator(ClientHandler coordinator) {
        this.coordinator = coordinator;
    }

    void setCoordinator(boolean isCoordinator) {
        if (isCoordinator) {
            coordinator = this;
        }
    }

    String getClientId() {
        return clientId;
    }

    Socket getSocket() {
        return socket;
    }
}
