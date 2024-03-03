package Chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // create a thread pool to manage client connection threads
    private static ExecutorService executor = Executors.newCachedThreadPool();
    // create a CopyOnWriteArrayList to store client handlers with thread safely
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        // try to open a server socket on port 7676
        try (ServerSocket serverSocket = new ServerSocket(7575);) {
            System.out.println("Server initialized, waiting for connections...");

            //  listen new client connections continuously
            while (true) {
                // accept client connect and create a socket
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress().getHostAddress());
                // create a new client handler with new socket
                ClientHandler clientHandler = new ClientHandler(socket);
                // add the new client to client handler list
                clients.add(clientHandler);
                // Execute client handler thread
                executor.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Inner class to handle each client connection
    private static class ClientHandler implements Runnable {
        // create socket for every client
        private Socket socket;
        // create reader to read client input
        private BufferedReader reader;
        // create writer to print output to client
        private PrintWriter writer;
        // create nickname for every client
        private String nickname;
        // initialize ClientHandler's constructor

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.nickname = "Anonymous";
        }

        @Override
        public void run() {
            try {
                writer.println("Welcome to Ping's Chat Application!");
                displayMenu();
                String message;
                // reads client messages continuously
                while ((message = reader.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                System.out.println(nickname + " has disconnected.");
            } finally {
                synchronized (clients) {
                    clients.remove(this);
                }
            }
        }
        private void displayMenu() {
            writer.println("Please enter your command.");
            writer.println("1. Set nickname, enter format: '/nick nickname'");
            writer.println("2. Send broadcast message, enter format: '/bc message'");
            writer.println("3. Send private message, enter format: '/msg nickname message'");
            writer.println("4. List all connected clients, enter '/list'");
            writer.println("5. Exit the Chat Application, enter '/quit'");
        }

        // handle message method
        private void handleMessage(String message) {
            if (message.startsWith("/nick ")) {
                setNickname(message);
            } else if (message.startsWith("/bc ")) {
                broadcastMessage(message.substring(4));
            } else if (message.startsWith("/msg ")) {
                sendPrivateMessage(message);
            } else if (message.equals("/list")) {
                listAllUsers();
            } else if (message.equals("/quit")) {
                quitChat();
            } else {
                writer.println("Unknown command. Please try again.");
            }
        }

        // set nickname method
        private void setNickname(String message) {
            String oldNickname = this.nickname;
            this.nickname = message.substring(6).trim();
            writer.println("Nickname " +oldNickname + " changed to: " + this.nickname);
        }

        // broadcast message method
        private void broadcastMessage(String message) {
            String fullMessage = this.nickname + ": " + message;
            // use for loop to iterate every client to send message
            for (ClientHandler client : clients) {
                client.writer.println(fullMessage);
            }
        }

        // send private message method
        private void sendPrivateMessage(String msg) {
            // find the first space position in the message
            int firstSpace = msg.indexOf(" ");
            // find the second space position in the message
            int secondSpace = msg.indexOf(" ", firstSpace + 1);
            // create found as a flag to indicate if the target user is found
            boolean found = false;
            if (firstSpace != -1 && secondSpace != -1) {
                // get user nickname
                String targetNickname = msg.substring(firstSpace + 1, secondSpace);
                // get user message
                String messageText = msg.substring(secondSpace + 1);
                for (ClientHandler client : clients) {
                    // if find target user, send message
                    if (client.nickname.equals(targetNickname)) {
                        client.writer.println("(Private) " + this.nickname + ": " + messageText);
                        found = true;
                        break;
                    }
                }
                // if not find target user, send a hint
                if(!found) {
                    writer.println("User " + targetNickname + " not found.");
                }
            } else {
                // if format has mistake
                writer.println("Command format error. Please enter: '/msg nickname message'");
            }
        }

        // list all connected clients method
        private void listAllUsers () {
            writer.println("Currently online: " + clients.stream().map(c -> c.nickname).reduce((a, b) -> a + ", " + b).orElse("No one else is online."));
        }

        // quit Chat Applicatioin method
        private void quitChat () {
            try {
                closeClientConnection();
            } catch (IOException e) {
                System.err.println("Error while closing client connection: " + e.getMessage());
            }
        }

        private void closeClientConnection () throws IOException {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            synchronized (clients) {
                clients.remove(this);
            }
            System.out.println(nickname + " disconnected.");
        }
    }
}

