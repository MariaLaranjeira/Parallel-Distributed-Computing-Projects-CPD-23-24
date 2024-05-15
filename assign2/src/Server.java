import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class Server {
    private int port;
    private boolean isRankMode;
    private int playersPerGame;

    private Queue<Client> playersQueue;

    private Map<String, Client> tokenMap;

    private Lock queueLock;
    private Lock tokenLock;

    private final int TIMEOUT = 10000; // to avoid slow clients

    public Server(int port, int playersPerGame, boolean isRankMode) {
        this.port = port;
        this.isRankMode = isRankMode;

        this.playersQueue = new LinkedList<>();

        this.tokenMap = new HashMap<>();

        this.queueLock = new ReentrantLock();
        this.tokenLock = new ReentrantLock();
    }

    public void startServer() throws IOException {
        //Start Server in port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port + " with " + (isRankMode? "rank" : "simple") + " mode");

            //Start new connections thread
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(TIMEOUT);
                Thread.startVirtualThread(() -> handleClient(clientSocket));
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    private void handleClient(Socket clientSocket) {
        try {
            System.out.println("New connection received: " + clientSocket.getInetAddress());
            Client newPlayer = authenticate(clientSocket);

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

    private Client authenticate(Socket clientSocket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        String token = reader.readLine();

        tokenLock.lock();
        try {
            if (token != null && tokenMap.containsKey(token)) {
                return tokenMap.get(token);  
            } else {
                Client newPlayer = new Client(clientSocket); 

                token = UUID.randomUUID().toString();

                tokenMap.put(token, newPlayer);

                writer.println(token);

                return newPlayer;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    private void startGame(List<Client> players) {
        List<Socket> sockets = new ArrayList<>();

        for (Client player : players) {
            sockets.add(player.getSocket());
        }

        Game game = new Game(sockets);

        new Thread(game).start();
    }

    private void performSimpleMatchmaking(Client player) {
        queueLock.lock();
        try {
            playersQueue.add(player);

            //verify if all members are ready
            if (playersQueue.size() >= playersPerGame) {
                List<Client> players = new ArrayList<>();
                for (int i = 0; i < playersPerGame; i++) {
                    players.add(playersQueue.poll());
                    System.out.println("PLayer removed from waiting queue");
                }

                //Start the game
                startGame(players);
            }
        } finally {
            queueLock.unlock();
        }
    }

    private void performRankedMatchmaking(Client newPlayer) {
        queueLock.lock();
        try {
            playersQueue.add(newPlayer);
        } finally {
            queueLock.unlock();
        }
    }


    public static void main(String[] args) throws IOException {
        // args: port numberPlayers mode  
        if (args.length != 3) {
            System.out.println("Usage: java Server <port> <numberPlayers> <mode>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int playersPerGame = Integer.parseInt(args[1]);
        String mode = args[2];
        boolean isRankMode = mode.equalsIgnoreCase("rank");

        Server server = new Server(port, playersPerGame, isRankMode);
        server.startServer();
    }

}