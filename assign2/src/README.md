# UNO Game Server and Client

This project implements a text-based UNO game using Java. It supports both simple and ranked modes, with fault tolerance for client disconnections.

## How to Run the Project

### Compilation

First, compile all the Java files using the following command:

```bash
javac *.java
```

To start the server, use the following command:

```bash
java Server <port> <numberPlayers> <mode>
```

<port>: The port number on which the server will listen for connections.
<numberPlayers>: The number of players required to start a game.
<mode>: The mode of the game, either rank or simple.

To start a client, use the following command:

```bash
java Client <hostname> <serverPort>
```

<hostname>: The hostname or IP address of the server.
<serverPort>: The port number on which the server is listening.

## Project Structure and Class Roles

### Server.java
This class handles the server-side logic of the game. It manages client connections, authentication, registration, and matchmaking. It also handles fault tolerance by allowing clients to reconnect with their token.

### Client.java
This class handles the client-side logic of the game. It manages user input for login, registration, and gameplay. It also supports reconnecting to the server with a token.

### Game.java
This class represents the game logic. It manages the game state, including player hands, the deck, and the discard pile. It handles player turns and determines the winner.

### UnoCard.java
This class represents an UNO card. It has attributes for the card's color and value and provides methods to get these attributes.

### PasswordUtils.java
This utility class handles password hashing and verification. It uses SHA-256 to hash passwords and provides methods to verify passwords.

### Game Modes
**Simple Mode:**
In simple mode, players are matched without considering their ranks. The game starts as soon as the required number of players are connected.

**Ranked Mode:**
In ranked mode, players are matched based on their ranks. The rank difference threshold can be relaxed over time to allow players with larger rank differences to play together if they have been waiting for a long time. After a game, the ranks are accordingly updated.

### Fault Tolerance
The server supports fault tolerance by allowing clients to reconnect using their token. When a client disconnects, they are removed from the queue, but they can rejoin with the same token and resume their position in the queue.

## How to Play
Example:
```bash
Server: Your turn! Current card: G_3
Server: Your Deck: Y_1, B_4, B_6, Y_3, B_1
Your Play:
```
Players must choose a card from their deck that either matches the current card's color or number. If the player doesn't have any cards on their deck that fullfil the requirements they must write **DECK** and the server will draw a random card for them from the main deck.
For debug and testing purposes if a player writes **WIN_DEBUG** they instantly win the game. Sometimes the player may write a card or one of the 2 key phrases as a valid input but the game rejects it as so. In that case, the user must replay it and it will now work. This is a bug we didn't get to solve which involves the server sometimes "eating" the first character of the input.