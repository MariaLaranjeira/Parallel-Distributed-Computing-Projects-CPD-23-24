import java.io.*;
import java.net.*;

public class Client {
    private String serverAddress;
    private int serverPort;

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void startClient() throws IOException {
        Socket socket = new Socket(serverAddress, serverPort);
        System.out.println("Connected to the game server");

        // Authentication and communication with server
        OutputStream output = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(output, true);

        // Example authentication
        writer.println("USERNAME:PASSWORD");

        // Handle game logic and communication from server
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String message;
        while ((message = reader.readLine()) != null) {
            System.out.println("Server says: " + message);
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        GameClient client = new GameClient("localhost", 12345);
        client.startClient();
    }
}