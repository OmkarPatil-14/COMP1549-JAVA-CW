import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Client class
public class Client {
    // Change the CLIENT_ID value to set a different client ID
    private static String CLIENT_ID = "omkar1344";

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter server IP address: ");
            String serverIP = scanner.nextLine();
            System.out.print("Enter server port: ");
            int serverPort = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            Socket socket = new Socket(serverIP, serverPort);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Your unique ID is " + CLIENT_ID);

            new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            String userInput;
            while ((userInput = scanner.nextLine()) != null) {
                writer.println(userInput);
                if (userInput.equals("/quit")) {
                    break;
                }
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("Server not found. Please check the IP address and port.");
            e.printStackTrace();
        }
    }
}