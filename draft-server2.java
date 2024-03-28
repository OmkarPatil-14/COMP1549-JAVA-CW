import java.io.*;
import java.net.*;
import java.util.*;

public class Serverx {
    private static final int PORT = 12349;
    private static List<ClientHandlerx> clients = new ArrayList<>();
    private static ClientHandlerx coordinator = null;


    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Assign coordinator if this is the first client
                if (clients.isEmpty()) {
                    coordinator = new ClientHandlerx(clientSocket);
                    coordinator.setCoordinator(true);
                    coordinator.start();
                    System.out.println("Coordinator assigned: " + coordinator.getClientInfo());
                } else {
                    ClientHandlerx clientHandler = new ClientHandlerx(clientSocket);
                    clients.add(clientHandler);
                    clientHandler.start(); // Start the client handler thread
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeClient(ClientHandlerx client) {
        String clientInfo = client.getClientInfo();
        clients.remove(client);

        if (client == coordinator) {
            // If coordinator leaves, select new coordinator
            if (!clients.isEmpty()) {
                coordinator = clients.get(0);
                coordinator.setCoordinator(true);
                System.out.println("New coordinator selected: " + coordinator.getClientInfo());
                broadcastMessage("New coordinator selected: " + coordinator.getClientInfo(), null);
            } else {
                coordinator = null;
                System.out.println("No coordinator available");
            }
        }

        System.out.println("Client disconnected: " + client.getClientInfo());
        broadcastMessage(clientInfo + " disconnected", null);
    }
  public class ClientHandlerx extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static int coordinatorID = -1;
    private boolean isCoordinator = false;

    public ClientHandlerx(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from client: " + inputLine);
                // Handle coordinator selection if the current coordinator leaves
                if ("quit".equalsIgnoreCase(inputLine.trim())) {
                    System.out.println("Client disconnected: " + clientSocket);
                    break; // Exit the loop if the client sends "quit"
                }
                // Broadcast message to all clients except the sender
                Serverx.broadcastMessage(inputLine, this);
            }
        } catch (IOException e) {
            // Client disconnected
            System.out.println("Client disconnected: " + clientSocket);
        } finally {
            // Close resources if not already closed
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                Serverx.removeClient(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void setCoordinator(boolean isCoordinator) {
        this.isCoordinator = isCoordinator;
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }

    public static void informCoordinator(ClientHandlerx client) {
        if (coordinatorID != -1) {
            client.sendMessage("Coordinator ID: " + coordinatorID);
        }
    }

    public String getClientInfo() {
        return "Socket[addr=" + clientSocket.getInetAddress() +
                ",port=" + clientSocket.getPort() +
                ",localport=" + clientSocket.getLocalPort() + "]";
    }


    public static synchronized void broadcastMessage(String message, ClientHandlerx sender) {
        for (ClientHandlerx client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
}
