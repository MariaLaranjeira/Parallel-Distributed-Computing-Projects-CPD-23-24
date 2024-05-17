import java.net.*;
import java.io.*;

public class Client {
    private final Socket clientSocket;
    private int serverPort; 
    private String hostname; 
    private String token;
    private float rank;
    
    public Client(Socket clientSocket) {
        this.rank = 0;
        this.clientSocket = clientSocket;
    }

    public void startClient() throws IOException {
        try {
            System.out.println("Starting client on port: " + serverPort);

            clientSocket.setSoTimeout(5000);  // Match the server's timeout

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

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
                // !Include logic to handle game states and server messages
            }

        } catch (UnknownHostException ex) {
            
            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public Socket getSocket(){
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