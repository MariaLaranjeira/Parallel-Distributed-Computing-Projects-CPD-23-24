import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private int port;
    private List<Socket> playerSockets = new ArrayList<>();
    private int playersPerGame;

    public Server(int port, int playersPerGame) {
        this.port = port;
        this.playersPerGame = playersPerGame;
    }

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New connection received: " + clientSocket.getInetAddress());

            // Handle authentication
            if (authenticate(clientSocket)) {
                playerSockets.add(clientSocket);
                if (playerSockets.size() == playersPerGame) {
                    startGame(new ArrayList<>(playerSockets));
                    playerSockets.clear();
                }
            } else {
                clientSocket.close();
            }
        }
    }

    private boolean authenticate(Socket clientSocket) {
        // Implement authentication logic
        return true; // Assume always true for example purposes
    }

    private void startGame(List<Socket> gameSockets) {
        Game game = new Game(gameSockets);
        new Thread(game).start();
    }

    public static void main(String[] args) throws IOException {
        int port = 12345;
        int playersPerGame = 2; // e.g., for chess
        GameServer server = new GameServer(port, playersPerGame);
        server.startServer();
    }
}