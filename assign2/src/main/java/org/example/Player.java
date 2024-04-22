package org.example;

import java.net.Socket;

public class Player {
    private Socket socket;
    private int level;

    public Player(Socket socket, int level) {
        this.socket = socket;
        this.level = level;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}