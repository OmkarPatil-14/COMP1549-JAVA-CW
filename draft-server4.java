import java.io.*;
import java.util.*;

public class Server {
    private static final int PORT = 12346;
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start(); // Start the client handler thread

                // Assign coordinator if it's the first client
                if (clients.size() == 1) {
                    coordinatorIndex = 0; // First client becomes coordinator
                    clientHandler.setCoordinator(true);
                    System.out.println("Coordinator: " + clientHandler.getClientInfo());
                } else {
                    // Notify new client about current coordinator
                    clientHandler.sendMessage("Coordinator: " + clients.get(coordinatorIndex).getClientInfo());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(ClientHandler client) {
        String clientInfo = client.getClientInfo();
        clients.remove(client);

        if (clients.isEmpty()) {
            coordinatorIndex = -1;
        } else {
            if (clients.indexOf(client) == coordinatorIndex) {
                // If coordinator leaves, select new coordinator
                coordinatorIndex = 0;
                System.out.println("New coordinator selected: " + clients.get(coordinatorIndex).getClientInfo());
                broadcastMessage("New coordinator selected: " + clients.get(coordinatorIndex).getClientInfo(), null);
            }
            System.out.println("Client disconnected: " + client.getClientInfo());
            broadcastMessage(clientInfo + " disconnected", null);
        }
    }

    public static synchronized void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static synchronized void handleDisconnect(ClientHandler client) {
        clients.remove(client);
        if (clients.size() == 0) {
            coordinatorIndex = -1;
        } else if (clients.indexOf(client) == coordinatorIndex) {
            // If coordinator leaves, select new coordinator
            coordinatorIndex = 0;
            System.out.println("New coordinator selected: " + clients.get(coordinatorIndex).getClientInfo());
            broadcastMessage("New coordinator selected: " + clients.get(coordinatorIndex).getClientInfo(), null);
        }
    }
        }

        // Get client info
        public String getClientInfo() {
            return "Socket[addr=" + clientSocket.getInetAddress() +
                    ",port=" + clientSocket.getPort() +
                    ",localport=" + clientSocket.getLocalPort() + "]";
        }
    }
}
