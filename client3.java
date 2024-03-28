import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Client class
public class client3 {
    private static Socket socket;
    private static PrintWriter writer;
    private static boolean interrupted = false;

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter server IP address: ");
            String serverIP = scanner.nextLine();
            System.out.print("Enter server port: ");
            int serverPort = scanner.nextInt();
            scanner.nextLine();

            System.out.print("Enter your unique ID: ");
            String clientId = scanner.nextLine();

            socket = new Socket(serverIP, serverPort);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            writer.println(clientId); // Send the client ID to the server

            System.out.println("Your unique ID is " + clientId);

            Thread receivingThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    if (!interrupted) {
                        e.printStackTrace();
                    }
                }
            });
            receivingThread.start();

            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                interrupted = true;
                closeConnection();
            }));

            String userInput;
            while ((userInput = scanner.nextLine()) != null) {
                writer.println(userInput);
                if (userInput.equals("/quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Server not found. Please check the IP address and port.");
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private static void closeConnection() {
        try {
            if (writer != null) {
                writer.println("/quit"); // Send the /quit command to notify the server
                writer.flush();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            if (!interrupted) {
                e.printStackTrace();
            }
        }
    }
}
