import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12346;
    private static List<ClientHandler> clients = new ArrayList<>();
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

    
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean isCoordinator; // Add coordinator flag

        public ClientHandler(Socket clientSocket) {
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
                    Server.broadcastMessage(inputLine, this);
                    if ("quit".equalsIgnoreCase(inputLine.trim())) {
                        System.out.println("Quit command detected, exiting loop."); // Debug statement
                        break; // Exit the loop if the client sends "quit"
                    }
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
                    Server.removeClient(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        // Set coordinator flag
        public void setCoordinator(boolean isCoordinator) {
            this.isCoordinator = isCoordinator;
        }

        // Get client info
        public String getClientInfo() {
            return "Socket[addr=" + clientSocket.getInetAddress() +
                    ",port=" + clientSocket.getPort() +
                    ",localport=" + clientSocket.getLocalPort() + "]";
        }
    }
}
