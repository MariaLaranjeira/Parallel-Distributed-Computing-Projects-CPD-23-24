import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class Server {
    private int port;
    private Queue<Player> playersQueue;
    private Map<String, Player> tokenMap;
    private Lock queueLock = new ReentrantLock();
    private Lock tokenLock = new ReentrantLock();
    private int playersPerGame;
    private boolean isRankMode;

    public Server(int port, int playersPerGame, boolean isRankMode) {
        this.port = port;
        this.playersPerGame = playersPerGame;
        this.isRankMode = isRankMode;
        this.playersQueue = new LinkedList<>();
        this.tokenMap = new HashMap<>();
    }

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            // Set a timeout to avoid slow clients blocking the server
            clientSocket.setSoTimeout(5000); // Timeout after 5000 milliseconds of inactivity

            // Use virtual threads (lightweight threads introduced in Java SE 21)
            Thread.startVirtualThread(() -> handleClient(clientSocket));
        }
    }


    private void handleClient(Socket clientSocket) {
        try {
            System.out.println("New connection received: " + clientSocket.getInetAddress());

            Player newPlayer = authenticate(clientSocket);
            if (newPlayer != null) {
                if (isRankMode) {
                    performRankedMatchmaking(newPlayer);
                } else {
                    performSimpleMatchmaking(newPlayer);
                }
            } else {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error handling client socket: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println("Failed to close client socket: " + ex.getMessage());
            }
        }
    }

    private Player authenticate(Socket clientSocket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        String token = reader.readLine();
        tokenLock.lock();
        try {
            if (token != null && tokenMap.containsKey(token)) {
                return tokenMap.get(token);  // Reconnected player
            } else {
                // New authentication logic here, return new player with generated token
                Player newPlayer = new Player(clientSocket, 0); // Assume level 0 for simplicity
                token = UUID.randomUUID().toString();
                tokenMap.put(token, newPlayer);
                writer.println(token); // Send token back to client
                return newPlayer;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    private void startGame(List<Player> players) {
        List<Socket> sockets = new ArrayList<>();
        for (Player player : players) {
            sockets.add(player.getSocket());
        }
        Game game = new Game(sockets);
        new Thread(game).start();
    }

    private void performSimpleMatchmaking(Player player) {
        queueLock.lock();
        try {
            playersQueue.add(player);
            if (playersQueue.size() >= playersPerGame) {
                List<Player> players = new ArrayList<>();
                for (int i = 0; i < playersPerGame; i++) {
                    players.add(playersQueue.poll());
                }
                startGame(players);
            }
        } finally {
            queueLock.unlock();
        }
    }

    private void performRankedMatchmaking(Player newPlayer) {
        queueLock.lock();
        try {
            // Implement matchmaking logic that accounts for player levels
            // This is a simple placeholder for actual rank-based logic
            playersQueue.add(newPlayer);
        } finally {
            queueLock.unlock();
        }
    }


    public static void main(String[] args) throws IOException {
        int port = 12345;
        boolean isRankMode = args.length > 0 && args[0].equals("rank");
        int playersPerGame = args.length > 1 ? Integer.parseInt(args[1]) : 2; // Default to 2 if not specified
        GameServer server = new GameServer(port, playersPerGame, isRankMode);
        server.startServer();
    }
}