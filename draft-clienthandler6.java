import java.io.*;
import java.net.*;

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

    public static void main(String[] args) {
        // This is a placeholder main method.
        // ClientHandler is not meant to be run independently.
        // It is typically used within a server application to handle client connections.
        // You can add any necessary setup or additional code here.
    }
}
