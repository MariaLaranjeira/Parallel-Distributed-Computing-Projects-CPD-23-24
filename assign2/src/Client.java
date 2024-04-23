package org.example;

import java.net.*;
import java.io.*;

public class Client {
    private String serverAddress; //Hostname
    private int serverPort; //Port

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
                // !Include logic to handle game states and server messages
            }

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) return;
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

        Client client = new Client(hostname, port);
        client.startClient();
    }
}