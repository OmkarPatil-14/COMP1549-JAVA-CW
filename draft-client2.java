
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Clientx {
    private static final String SERVER_IP = "127.0.0.1"; //
    private static final int SERVER_PORT = 12349;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Prompt the user to input client ID, server IP, and port number
        System.out.println("Enter your client ID:");
        String clientID = scanner.nextLine();
        System.out.println("Enter server IP address (default: " + SERVER_IP + "):");
        String serverIP = scanner.nextLine();
        if (serverIP.isEmpty()) {
            serverIP = SERVER_IP; // Use default server IP if user input is empty
        }
        System.out.println("Enter server port number (default: " + SERVER_PORT + "):");
        int serverPort = Integer.parseInt(scanner.nextLine());
        if (serverPort == 0) {
            serverPort = SERVER_PORT; // Use default server port if user input is empty
        }

        // Connect to the server
        try (Socket socket = new Socket(serverIP, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send client ID to the server to join the network
            out.println(clientID);

            // Receive information about the coordinator from the server
            String coordinatorInfo = in.readLine();
            System.out.println("Received coordinator information: " + coordinatorInfo);

            // Start sending and receiving messages
            Thread receiveThread = new Thread(() -> {
                String serverResponse;
                try {
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println(serverResponse);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            receiveThread.start();

            // Send messages to the server and other clients
            String userInput;
            while (true) {
                userInput = scanner.nextLine();
                out.println(userInput); // Send the message to the server
                if ("quit".equalsIgnoreCase(userInput)) {
                    System.out.println("Quit command detected, exiting from server.");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
