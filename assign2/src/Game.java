import java.net.Socket;
import java.util.List;

public class Game implements Runnable {
    private List<Socket> userSockets;

    public Game(List<Socket> userSockets) {
        this.userSockets = userSockets;
    }

    @Override
    public void run() {
        start();
    }

    public void start() {
        System.out.println("Starting game with " + userSockets.size() + " players");
        // Game logic here
    }
}