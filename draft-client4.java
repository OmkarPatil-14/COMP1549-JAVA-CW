import java.io.*;
import java.util.Scanner;

public class Client2 {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12346;
    private static final String CLIENT_IDENTIFIER = "Client 2";

    public static void main(String[] args) {
        new Client2().connectToServer();
    }

    public void connectToServer() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            // Start a new thread to listen for messages from the server
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

            // Start sending messages to the server
            String userInput;
            while (true) {
                userInput = scanner.nextLine();
                out.println(CLIENT_IDENTIFIER + ": " + userInput); // Include sender identifier
                if ("quit".equalsIgnoreCase(userInput)) {
                    System.out.println("Quit command detected ,exiting from server.");
                    break;
                }
            }
        }
    }
}
