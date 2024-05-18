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
    private Map<String, String> userCredentials; // Temporary -> must upgrade to file
    private Lock queueLock;
    private Lock tokenLock;
    private final int TIMEOUT = 10000; // to avoid slow clients

    public Server(int port, int playersPerGame, boolean isRankMode) {
        this.port = port;
        this.isRankMode = isRankMode;
        this.playersQueue = new LinkedList<>();
        this.tokenMap = new HashMap<>();
        this.userCredentials = new HashMap<>();
        this.queueLock = new ReentrantLock();
        this.tokenLock = new ReentrantLock();

        // Test
        userCredentials.put("gigi", "1234");
        userCredentials.put("gerge", "4321");
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port + " with " + (isRankMode ? "rank" : "simple") + " mode");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(TIMEOUT);
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

            String action = reader.readLine();
            if ("LOGIN".equals(action)) {
                if (authenticate(reader, writer)) {
                    Client newPlayer = authorize(clientSocket);
                    if (newPlayer != null) {
                        handleMatchmaking(newPlayer);
                    } else {
                        clientSocket.close();
                    }
                } else {
                    writer.println("AUTH_FAILED");
                    clientSocket.close();
                }
            } else if ("REGISTER".equals(action)) {
                if (register(reader, writer)) {
                    Client newPlayer = authorize(clientSocket);
                    if (newPlayer != null) {
                        handleMatchmaking(newPlayer);
                    } else {
                        clientSocket.close();
                    }
                } else {
                    writer.println("REGISTRATION_FAILED");
                    clientSocket.close();
                }
            } else {
                writer.println("INVALID_ACTION");
                clientSocket.close();
            }

        } catch (IOException e) {
            System.out.println("Error handling client socket: " + e.getMessage());
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println("Failed to close client socket: " + ex.getMessage());
            }
        }
    }

    private boolean authenticate(BufferedReader reader, PrintWriter writer) throws IOException {
        String username = reader.readLine();
        String password = reader.readLine();
        System.out.println("Received credentials: " + username + "/" + password);

        if (username != null && password != null && password.equals(userCredentials.get(username))) {
            writer.println("AUTH_SUCCESS");
            return true;
        } else {
            writer.println("AUTH_FAILED");
            return false;
        }
    }

    private boolean register(BufferedReader reader, PrintWriter writer) throws IOException {
        String username = reader.readLine();
        String password = reader.readLine();
        System.out.println("Received registration: " + username + "/" + password);

        if (username != null && password != null && !userCredentials.containsKey(username)) {
            userCredentials.put(username, password);
            writer.println("REG_SUCCESS");
            return true;
        } else {
            writer.println("REGISTRATION_FAILED");
            return false;
        }
    }

    private Client authorize(Socket clientSocket) throws IOException {
        System.out.println("Authorizing...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        System.out.println("Waiting for token from client...");
        String token = reader.readLine();
        System.out.println("Received token: " + token);

        tokenLock.lock();
        try {
            if (token != null && tokenMap.containsKey(token)) {
                System.out.println("Existing token found, returning associated client.");
                return tokenMap.get(token);
            } else {
                System.out.println("No existing token, creating new client.");
                Client newPlayer = new Client(clientSocket);
                token = UUID.randomUUID().toString();
                tokenMap.put(token, newPlayer);
                writer.println(token);
                System.out.println("New token assigned: " + token);
                return newPlayer;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    private void handleMatchmaking(Client newPlayer) {
        if (isRankMode) {
            performRankedMatchmaking(newPlayer);
        } else {
            performSimpleMatchmaking(newPlayer);
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
            System.out.println("Player added to queue. Queue size: " + playersQueue.size());
            if (playersQueue.size() >= playersPerGame) {
                List<Client> players = new ArrayList<>();
                for (int i = 0; i < playersPerGame; i++) {
                    players.add(playersQueue.poll());
                    System.out.println("Player removed from waiting queue");
                }
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
            System.out.println("Player added to ranked queue. Queue size: " + playersQueue.size());
        } finally {
            queueLock.unlock();
        }
    }

    public static void main(String[] args) {
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
