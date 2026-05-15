package blackjack;

/*
Qui il server:
 apre una porta
 aspetta un client
 logga la connessione
 riceve i comandi
 stampa i log 
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BlackjackServer {
    public static void main(String[] args) {
        int port = 5000;

        System.out.println("Server Blackjack avviato sulla porta " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("In attesa di un client...");

            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                System.out.println("Connessione avvenuta con successo con client: "
                        + clientSocket.getInetAddress().getHostAddress());

                BlackjackGame game = new BlackjackGame();

                System.out.println("Nuova partita creata.");
                System.out.println("Totale iniziale giocatore: " + game.getPlayerValue());

                sendMessage(out, game.getInitialState());

                String command;
                while ((command = in.readLine()) != null) {
                    System.out.println("Ricevuto messaggio dal client: " + command);

                    if (command.equalsIgnoreCase("exit")) {
                        System.out.println("Client ha chiuso la connessione.");
                        sendMessage(out, "Connessione chiusa.");
                        break;
                    }

                    String response = game.handleCommand(command);
                    sendMessage(out, response);

                    if (command.equalsIgnoreCase("hit")) {
                        System.out.println("Elaborato HIT. Valore giocatore: " + game.getPlayerValue());
                        if (game.isGameOver()) {
                            System.out.println("Client ha perso per sballo.");
                            System.out.println("Fine gioco.");
                        }
                    }

                    if (command.equalsIgnoreCase("stand")) {
                        System.out.println("Elaborato STAND.");
                        if (game.isGameOver()) {
                            System.out.println("Fine gioco.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(PrintWriter out, String message) {
        String[] lines = message.split("\n");
        for (String line : lines) {
            out.println(line);
        }
        out.println("END");
    }
}