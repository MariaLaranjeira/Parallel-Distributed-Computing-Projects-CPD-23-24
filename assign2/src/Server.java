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
    private Map<String, String> userCredentials;
    private Map<String, String> tokenToUsernameMap; // Map to store token to username mapping
    private Set<String> loggedInUsers;  // Track logged-in users
    private Lock queueLock;
    private Lock tokenLock;
    private Lock loginLock;  // Lock for login operations
    private final int TIMEOUT = 30000; // to avoid slow clients
    private final String credentialsFilePath = "../database/user_credentials.txt";
    private int rankDifferenceThreshold = 2; // Initial threshold
    private final int maxRankDifferenceThreshold = 10; // Maximum allowed difference
    private final int thresholdIncreaseInterval = 10000; // Increase interval in milliseconds
    private boolean gameRunning;

    public Server(int port, int playersPerGame, boolean isRankMode) {
        this.port = port;
        this.isRankMode = isRankMode;
        this.playersPerGame = playersPerGame;
        this.playersQueue = new LinkedList<>();
        this.tokenMap = new HashMap<>();
        this.userCredentials = new HashMap<>();
        this.tokenToUsernameMap = new HashMap<>(); // Initialize the map
        this.loggedInUsers = new HashSet<>();
        this.queueLock = new ReentrantLock();
        this.tokenLock = new ReentrantLock();
        this.loginLock = new ReentrantLock();
        this.gameRunning = false;

        // Load user credentials from file
        loadUserCredentials();

        // Start the rank difference relaxation thread if in rank mode
        if (isRankMode) {
            startRankDifferenceRelaxation();
        }
    }

    private void loadUserCredentials() {
        try (BufferedReader br = new BufferedReader(new FileReader(credentialsFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {  // Expecting format: username:hashedPassword:rank
                    userCredentials.put(parts[0], parts[1] + ":" + parts[2]);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read credentials file: " + e.getMessage());
        }
    }

    private void saveUserCredentials() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(credentialsFilePath))) {
            for (Map.Entry<String, String> entry : userCredentials.entrySet()) {
                pw.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            System.out.println("Could not write to credentials file: " + e.getMessage());
        }
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
                    Client newPlayer = authorize(clientSocket, reader);
                    if (newPlayer != null) {
                        writer.println(newPlayer.getRank());
                        handleMatchmaking(newPlayer);
                        waitForClientDisconnection(clientSocket, newPlayer);  // Monitor client disconnection
                    } else {
                        clientSocket.close();
                    }
                } else {
                    writer.println("AUTH_FAILED");
                    clientSocket.close();
                }
            } else if ("REGISTER".equals(action)) {
                if (register(reader, writer)) {
                    Client newPlayer = authorize(clientSocket, reader);
                    if (newPlayer != null) {
                        writer.println(newPlayer.getRank());
                        handleMatchmaking(newPlayer);
                        waitForClientDisconnection(clientSocket, newPlayer);  // Monitor client disconnection
                    } else {
                        clientSocket.close();
                    }
                } else {
                    writer.println("REGISTRATION_FAILED");
                    clientSocket.close();
                }
            } else if ("RECONNECT".equals(action)) {
                if (reconnect(reader, writer, clientSocket)) {
                    writer.println("RECONNECT_SUCCESS");
                    handleReconnectionMatchmaking(writer, clientSocket);
                    waitForClientDisconnection(clientSocket, tokenMap.get(getTokenBySocket(clientSocket)));  // Monitor client disconnection
                } else {
                    writer.println("RECONNECT_FAILED");
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

    private void waitForClientDisconnection(Socket clientSocket, Client client) {
        Thread.startVirtualThread(() -> {
            try {
                clientSocket.getInputStream().read();
            } catch (IOException e) {
                handleClientDisconnection(client);
            }
        });
    }

    private void handleClientDisconnection(Client client) {
        String token = getTokenBySocket(client.getSocket());
        String username = tokenToUsernameMap.get(token);

        if (username != null) {
            loginLock.lock();
            try {
                loggedInUsers.remove(username);
                System.out.println("User " + username + " removed from logged-in users due to disconnection.");
            } finally {
                loginLock.unlock();
            }

            queueLock.lock();
            try {
                playersQueue.remove(client);
                System.out.println("User " + username + " removed from the queue due to disconnection.");
            } finally {
                queueLock.unlock();
            }
        }
    }

    private boolean authenticate(BufferedReader reader, PrintWriter writer) throws IOException {
        String username = reader.readLine();
        String password = reader.readLine();
        String hashedPassword = PasswordUtils.hashPassword(password);
        System.out.println("Received credentials: " + username + "/" + hashedPassword);

        loginLock.lock();
        try {
            if (loggedInUsers.contains(username)) {
                writer.println("USER_ALREADY_LOGGED_IN");
                return false;
            }

            String storedValue = userCredentials.get(username);
            if (storedValue != null) {
                String[] parts = storedValue.split(":");
                if (parts.length == 2 && parts[0].equals(hashedPassword)) {
                    loggedInUsers.add(username);
                    writer.println("AUTH_SUCCESS");
                    return true;
                }
            }
            writer.println("AUTH_FAILED");
            return false;
        } finally {
            loginLock.unlock();
        }
    }

    private boolean register(BufferedReader reader, PrintWriter writer) throws IOException {
        String username = reader.readLine();
        String password = reader.readLine();
        String hashedPassword = PasswordUtils.hashPassword(password);
        System.out.println("Received registration: " + username + "/" + hashedPassword);

        loginLock.lock();
        try {
            if (loggedInUsers.contains(username)) {
                writer.println("USER_ALREADY_LOGGED_IN");
                return false;
            }

            if (username != null && password != null && !userCredentials.containsKey(username)) {
                userCredentials.put(username, hashedPassword + ":0"); // Initialize rank to 0
                saveUserCredentials();  // Save credentials to file after registration
                loggedInUsers.add(username);
                writer.println("REG_SUCCESS");
                return true;
            } else {
                writer.println("REGISTRATION_FAILED");
                return false;
            }
        } finally {
            loginLock.unlock();
        }
    }

    private boolean reconnect(BufferedReader reader, PrintWriter writer, Socket clientSocket) throws IOException {
        String username = reader.readLine();
        String token = reader.readLine();

        System.out.println("Reconnecting user: " + username + " with token: " + token);

        tokenLock.lock();
        try {
            if (tokenMap.containsKey(token)) {
                Client client = tokenMap.get(token);
                client.setSocket(clientSocket);  // Update the socket with the new connection
                loggedInUsers.add(username);
                writer.println("RECONNECT_SUCCESS");
                writer.println(client.getRank());
                System.out.println("User " + username + " reconnected successfully.");
                return true;
            } else {
                writer.println("RECONNECT_FAILED");
                System.out.println("Reconnection failed for user " + username + " with token " + token);
                return false;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    private Client authorize(Socket clientSocket, BufferedReader reader) throws IOException {
        System.out.println("Authorizing...");
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        System.out.println("Waiting for username from client...");
        String username = reader.readLine();
        System.out.println("Received username: " + username);

        System.out.println("Waiting for token from client...");
        String token = reader.readLine();
        System.out.println("Received token: " + token);

        tokenLock.lock();
        try {
            if (token != null && tokenMap.containsKey(token)) {
                System.out.println("Existing token found, returning associated client.");
                Client client = tokenMap.get(token);
                client.setSocket(clientSocket);  // Update the socket with the new connection
                tokenToUsernameMap.put(token, username);  // Map the token to the username
                return client;
            } else {
                System.out.println("No existing token, creating new client.");
                Client newPlayer = new Client(clientSocket);
                newPlayer.setRank(getRankByUsername(username));  // Assign the rank from the stored credentials
                token = UUID.randomUUID().toString();
                tokenMap.put(token, newPlayer);
                tokenToUsernameMap.put(token, username);  // Map the token to the username
                writer.println(token);
                System.out.println("New token assigned: " + token);
                return newPlayer;
            }
        } finally {
            tokenLock.unlock();
        }
    }

    private int getRankByUsername(String username) {
        String storedValue = userCredentials.get(username);
        if (storedValue != null) {
            String[] parts = storedValue.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[1]);
            }
        }
        return 0;
    }

    private void handleMatchmaking(Client newPlayer) {
        if (isRankMode) {
            performRankedMatchmaking(newPlayer);
        } else {
            performSimpleMatchmaking(newPlayer);
        }
    }

    private void handleReconnectionMatchmaking(PrintWriter writer, Socket clientSocket) {
        tokenLock.lock();
        try {
            String token = getTokenBySocket(clientSocket);
            Client client = tokenMap.get(token);
            if (client != null) {
                handleMatchmaking(client);
            }
        } finally {
            tokenLock.unlock();
        }
    }

    private void startGame(List<Client> players) {
        gameRunning = true;
        List<Socket> sockets = new ArrayList<>();
        for (Client player : players) {
            sockets.add(player.getSocket());
        }
        Game game = new Game(sockets, players, this, isRankMode);
        Thread.startVirtualThread(game);
    }

    private void performSimpleMatchmaking(Client player) {
        queueLock.lock();
        try {
            if (!playersQueue.contains(player)) {  // Add player to queue if not already present
                playersQueue.add(player);
                System.out.println("Player added to queue. Queue size: " + playersQueue.size());
            }
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
            if (!playersQueue.contains(newPlayer)) {  // Add player to queue if not already present
                playersQueue.add(newPlayer);
                System.out.println("Player added to ranked queue. Queue size: " + playersQueue.size());
            }

            List<Client> match = new ArrayList<>();
            for (Client player : playersQueue) {
                if (match.size() < playersPerGame && Math.abs(player.getRank() - newPlayer.getRank()) <= rankDifferenceThreshold) {
                    match.add(player);
                }
            }
            if (match.size() == playersPerGame) {
                playersQueue.removeAll(match);
                startGame(match);
            }
        } finally {
            queueLock.unlock();
        }
    }

    private void startRankDifferenceRelaxation() {
        Thread.startVirtualThread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    queueLock.lock();
                    try {
                        if (playersQueue.size() >= playersPerGame && !gameRunning) {
                            while (rankDifferenceThreshold < maxRankDifferenceThreshold && !gameRunning) {
                                Thread.sleep(thresholdIncreaseInterval);
                                rankDifferenceThreshold++;
                                System.out.println("Rank difference threshold increased to: " + rankDifferenceThreshold);
                                retryMatchmaking(); // Retry matchmaking with the new threshold
                            }
                            if (gameRunning) {
                                // Reset the threshold and wait until the game is over to restart the relaxation
                                rankDifferenceThreshold = 2;
                                while (gameRunning) {
                                    Thread.sleep(thresholdIncreaseInterval);
                                }
                            }
                        }
                    } finally {
                        queueLock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Rank difference relaxation thread interrupted: " + e.getMessage());
            }
        });
    }

    private void retryMatchmaking() {
        queueLock.lock();
        try {
            Queue<Client> tempQueue = new LinkedList<>(playersQueue);
            playersQueue.clear();
            while (!tempQueue.isEmpty()) {
                Client player = tempQueue.poll();
                handleMatchmaking(player);
            }
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

    public void updatePlayerRanks(Client winner, List<Client> players) {
        if (isRankMode) {  // Only update ranks in ranked mode
            winner.setRank(winner.getRank() + 1);
            for (Client player : players) {
                if (player != winner) {
                    player.setRank(player.getRank() - 1);
                }
            }
            // Save updated ranks to the credentials file
            for (Client player : players) {
                String token = getTokenBySocket(player.getSocket());
                String username = tokenToUsernameMap.get(token);
                if (username != null) {
                    String storedValue = userCredentials.get(username);
                    if (storedValue != null) {
                        String hashedPassword = storedValue.split(":")[0];
                        userCredentials.put(username, hashedPassword + ":" + player.getRank());
                    } else {
                        System.out.println("Error: No stored value found for username " + username);
                    }
                } else {
                    System.out.println("Error: Username not found for player with socket " + player.getSocket());
                }
            }
            saveUserCredentials();
        }
        gameRunning = false; // Mark the game as not running anymore
    }

    private String getTokenBySocket(Socket socket) {
        for (Map.Entry<String, Client> entry : tokenMap.entrySet()) {
            if (entry.getValue().getSocket().equals(socket)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
