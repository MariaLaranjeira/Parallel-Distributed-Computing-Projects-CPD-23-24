import java.io.*;
import java.net.*;

public class Client {
    private String serverAddress;
    private int serverPort;
    private int serverPort;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void startClient() throws IOException {
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            socket.setSoTimeout(5000);  // Match the server's timeout

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send token if it exists, otherwise signal a new connection
            out.println((token != null) ? token : "NEW");

            // Read the token assigned by the server or the updated one
            token = in.readLine();
            System.out.println("Connected with token: " + token);

            // Continue with other communication
            String fromServer;
            while ((fromServer = in.readLine()) != null) {
                System.out.println("Server: " + fromServer);
                // Include logic to handle game states and server messages
            }
        } catch (IOException e) {
            System.out.println("Error communicating with the server: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        GameClient client = new GameClient("localhost", 12345);
        client.startClient();
    }
}