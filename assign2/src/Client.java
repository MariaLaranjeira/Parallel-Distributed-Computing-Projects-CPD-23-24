import java.net.*;
import java.util.Scanner;
import java.io.*;

public class Client {
    private Socket clientSocket;
    private String token;
    private int rank;
    private String username;

    public Client(Socket clientSocket) {
        this.rank = 0; // Initialize rank to 0
        this.clientSocket = clientSocket;
    }

    public void startClient() throws IOException {
        try {
            connectToServer();
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private void connectToServer() throws IOException {
        System.out.println("Starting client on port: " + clientSocket.getPort());

        //clientSocket.setSoTimeout(5000);  // Match the server's timeout

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        Scanner scanner = new Scanner(System.in);

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
        username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        // Send username and password to the server
        out.println(username);
        out.println(password);

        String response = in.readLine();
        if ("AUTH_SUCCESS".equals(response) || "REG_SUCCESS".equals(response)) {
            System.out.println(response.equals("AUTH_SUCCESS") ? "Authentication successful" : "Registration successful");
            // Send username and token if it exists, otherwise signal a new connection
            String toSend = (token != null) ? token : "NEW";
            System.out.println("Sending username and token: " + username + " " + toSend);
            out.println(username);
            out.println(toSend);

            // Read the token assigned by the server or the updated one
            token = in.readLine();
            System.out.println("Connected with token: " + token);

            // Read the rank assigned by the server
            rank = Integer.parseInt(in.readLine());
            System.out.println("Your rank is: " + rank);

            // Continue with other communication
            String fromServer;
            while ((fromServer = in.readLine()) != null) {
                if (fromServer.startsWith("Your Play:")) {
                    System.out.print(fromServer);
                    String guess = scanner.nextLine();
                    out.println(guess);
                } else {
                    System.out.println("Server: " + fromServer);
                }
            }
        } else if ("USER_ALREADY_LOGGED_IN".equals(response)) {
            System.out.println("User already logged in.");
        } else {
            System.out.println("Operation failed: " + response);
        }
    }

    public void reconnectToServer() throws IOException {
        if (token != null && username != null) {
            System.out.println("Reconnecting to server with token: " + token);

            // Establish new socket connection
            this.clientSocket = new Socket(clientSocket.getInetAddress(), clientSocket.getPort());
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            out.println("RECONNECT");
            out.println(username);
            out.println(token);

            // Handle reconnection response
            String response = in.readLine();
            if ("RECONNECT_SUCCESS".equals(response)) {
                System.out.println("Reconnected successfully.");

                // Read the rank assigned by the server
                rank = Integer.parseInt(in.readLine());
                System.out.println("Your rank is: " + rank);

                // Continue with other communication
                String fromServer;
                Scanner scanner = new Scanner(System.in);
                while ((fromServer = in.readLine()) != null) {
                    if (fromServer.startsWith("Your Play:")) {
                        System.out.print(fromServer);
                        String guess = scanner.nextLine();
                        out.println(guess);
                    } else {
                        System.out.println("Server: " + fromServer);
                    }
                }
            } else {
                System.out.println("Reconnection failed: " + response);
            }
        } else {
            System.out.println("No token or username available for reconnection.");
        }
    }

    public Socket getSocket() {
        return this.clientSocket;
    }

    public void setSocket(Socket socket) {
        this.clientSocket = socket;
    }

    public int getRank() {
        return this.rank;
    }

    public void setRank(int rank) {
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

            client.reconnectToServer();

        } catch (IOException ex) {
            System.out.println("Error connecting to server: " + ex.getMessage());
        }
    }
}
