package Chat;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class Client {

        public static void main(String[] args) {
            try {
                // use localhost and port to connect server
                Socket socket = new Socket(InetAddress.getLocalHost(),7575);
                // create a BufferedReader to read server messages
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // create a PrintWriter to send messages to server
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // create a BufferedReader to read user input
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

                // start a new thread to read and print messages from the server continuously
                new Thread(() -> {
                    try {
                        String serverMessage;
                        while ((serverMessage = in.readLine()) != null) {
                            // print server messages to the console
                            System.out.println(serverMessage);
                        }
                    } catch (IOException e) {
                        System.out.println("Thank you for using Ping's Chat Application. Have a good day!");
                    }
                }).start();

                // [main thread] read user input, send it to the server
                String userInput;
                while ((userInput = consoleReader.readLine()) != null) {
                    // send user input to server
                    out.println(userInput);

                    // if user enters "/quit", disconnect from the server
                    if (userInput.equalsIgnoreCase("/quit")) {
                        System.out.println("Disconnecting from the server...");
                        break;
                    }
                }

                // close related resource
                out.close();
                in.close();
                consoleReader.close();
                socket.close();
            } catch (IOException e) {
                System.err.println("Error while closing client connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
}
