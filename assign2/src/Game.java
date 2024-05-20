import java.io.*;
import java.net.Socket;
import java.util.*;


public class Game implements Runnable {
    private List<Socket> userSockets;
    private Map<Socket, List<UnoCard>> playerHands;
    private List<UnoCard> deck;
    private List<UnoCard> discardPile;
    private int currentPlayerIndex;
    private UnoCard currentCard;
    private static final int INITIAL_HAND_SIZE = 5;
    private boolean gameRunning;
    private Server server;
    private List<Client> players;
    private boolean isRankMode;  // Add isRankMode field

    public Game(List<Socket> userSockets, List<Client> players, Server server, boolean isRankMode) {  // Add isRankMode parameter
        this.userSockets = userSockets;
        this.playerHands = new HashMap<>();
        this.deck = initializeDeck();
        this.discardPile = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.gameRunning = true;
        this.server = server;
        this.players = players;
        this.isRankMode = isRankMode;  // Initialize isRankMode field
    }

    @Override
    public void run() {
        try {
            startGame();
        } catch (IOException e) {
            System.out.println("Game error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<UnoCard> initializeDeck() {
        List<UnoCard> deck = new ArrayList<>();
        String[] colors = {"R", "Y", "G", "B"};
        String[] values = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

        for (String color : colors) {
            for (String value : values) {
                deck.add(new UnoCard(color, value));
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    private void dealInitialHands() {
        for (Socket player : userSockets) {
            List<UnoCard> hand = new ArrayList<>();
            for (int i = 0; i < INITIAL_HAND_SIZE; i++) {
                hand.add(deck.remove(deck.size() - 1));
            }
            playerHands.put(player, hand);
        }
    }

    private void startGame() throws IOException {
        dealInitialHands();
        broadcastMessage("Game started!");

        while (gameRunning) {
            playTurn();
        }
    }

    private void playTurn() throws IOException {
        Socket currentPlayerSocket = userSockets.get(currentPlayerIndex);
        PrintWriter out = new PrintWriter(currentPlayerSocket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(currentPlayerSocket.getInputStream()));

        out.println("Your turn! Current card: " + (currentCard != null ? currentCard : "None"));
        out.println("Your Deck: " + getPlayerHand(currentPlayerSocket));
        out.println("Your Play:");

        String play = in.readLine();
        if (play.equals("DECK")) {
            drawCard(currentPlayerSocket);
        } else if (play.equals("WIN_DEBUG")) {
            // Debug option to instantly win the game
            gameRunning = false;
            broadcastMessage("Player " + (currentPlayerIndex + 1) + " wins the game (DEBUG)!");
            server.updatePlayerRanks(getPlayerBySocket(currentPlayerSocket), players);
            return;
        } else {
            UnoCard playedCard = getCardFromPlay(play);
            if (playedCard == null || !isValidCard(play)) {
                out.println("Invalid play! Try again.");
                return;
            }
            if (currentCard == null || isValidPlay(playedCard)) {
                currentCard = playedCard;
                discardPile.add(playedCard);
                List<UnoCard> playerHand = playerHands.get(currentPlayerSocket);
                boolean removed = playerHand.remove(playedCard);  // Remove the played card from the player's hand

                // Debug statement to check card removal
                System.out.println("Attempting to remove " + playedCard + " from player hand: " + playerHand);
                System.out.println("Card removal successful: " + removed);

                broadcastMessage("Player " + (currentPlayerIndex + 1) + " played " + playedCard);

                if (playerHand.isEmpty()) {
                    gameRunning = false;
                    broadcastMessage("Player " + (currentPlayerIndex + 1) + " wins the game!");
                    server.updatePlayerRanks(getPlayerBySocket(currentPlayerSocket), players);
                    return;
                }
            } else {
                out.println("Invalid play! Try again.");
                return;
            }
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % userSockets.size();
    }

    private boolean isValidCard(String play) {
        return play.matches("[RYGB]_[0123456789]");
    }

    private boolean isValidPlay(UnoCard playedCard) {
        return playedCard.getColor().equals(currentCard.getColor()) || playedCard.getValue().equals(currentCard.getValue());
    }

    private UnoCard getCardFromPlay(String play) {
        String[] parts = play.split("_");
        if (parts.length != 2) {
            return null;
        }
        return new UnoCard(parts[0], parts[1]);
    }

    private void drawCard(Socket playerSocket) throws IOException {
        UnoCard drawnCard = deck.remove(deck.size() - 1);
        playerHands.get(playerSocket).add(drawnCard);
        PrintWriter out = new PrintWriter(playerSocket.getOutputStream(), true);
        out.println("You drew a card: " + drawnCard);
    }

    private String getPlayerHand(Socket playerSocket) {
        List<UnoCard> hand = playerHands.get(playerSocket);
        StringBuilder sb = new StringBuilder();
        for (UnoCard card : hand) {
            sb.append(card).append(", ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2); // Remove trailing comma and space
        }
        return sb.toString();
    }

    private void broadcastMessage(String message) throws IOException {
        for (Socket socket : userSockets) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        }
    }

    private Client getPlayerBySocket(Socket socket) {
        for (Client player : players) {
            if (player.getSocket().equals(socket)) {
                return player;
            }
        }
        return null;
    }
}
