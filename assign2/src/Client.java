import java.net.*;
import java.util.Scanner;
import java.io.*;

public class Client {
    private final Socket clientSocket;
    private String token;
    private float rank;

    public Client(Socket clientSocket) {
        this.rank = 0;
        this.clientSocket = clientSocket;
    }

    public void startClient() throws IOException {
        try {
            System.out.println("Starting client on port: " + clientSocket.getPort());

            //clientSocket.setSoTimeout(5000);  // Match the server's timeout

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Do you want to (1) Login or (2) Register? Enter 1 or 2: ");
                String choice = scanner.nextLine();
                if ("1".equals(choice)) {
                    out.println("LOGIN");
                } else if ("2".equals(choice)) {
                    out.println("REGISTER");
                } else {
                    System.out.println("Invalid choice.");
                    return;
                }

                System.out.print("Enter username: ");
                String username = scanner.nextLine();
                System.out.print("Enter password: ");
                String password = scanner.nextLine();

                // Send username and password to the server
                out.println(username);
                out.println(password);
            }

            String response = in.readLine();
            if ("AUTH_SUCCESS".equals(response) || "REG_SUCCESS".equals(response)) {
                System.out.println(response.equals("AUTH_SUCCESS") ? "Authentication successful" : "Registration successful");
                // Send token if it exists, otherwise signal a new connection
                String toSend = (token != null) ? token : "NEW";
                System.out.println("Sending token: " + toSend);
                out.println(toSend);

                // Read the token assigned by the server or the updated one
                token = in.readLine();
                System.out.println("Connected with token: " + token);

                // Continue with other communication
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    System.out.println("Server: " + fromServer);
                    // Include logic to handle game states and server messages
                }
            } else if ("USER_ALREADY_LOGGED_IN".equals(response)) {
                System.out.println("User already logged in.");
            } else {
                System.out.println("Operation failed: " + response);
            }

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public Socket getSocket() {
        return this.clientSocket;
    }

    public float getRank() {
        return this.rank;
    }

    public void setLevel(float rank) {
        this.rank = rank;
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Client <hostname> <serverPort>");
            return;
        }

        String hostname = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try {
            Socket clientSocket = new Socket(hostname, serverPort);
            Client client = new Client(clientSocket);
            client.startClient();
        } catch (IOException ex) {
            System.out.println("Error connecting to server: " + ex.getMessage());
        }
    }
}
