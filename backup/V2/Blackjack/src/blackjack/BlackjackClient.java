package blackjack;

/*
Il client:
 si connette al server;
 stampa i messaggi del server;
 fa scrivere hit, stand o exit da console.
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class BlackjackClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connesso al server " + host + ":" + port);
            System.out.println();

            readBlock(in);

            while (true) {
                System.out.print("> ");
                String command = scanner.nextLine().trim();

                out.println(command);

                String fullResponse = readBlock(in);

                if (command.equalsIgnoreCase("exit")) {
                    break;
                }

                if (isEndGameResponse(fullResponse)) {
                    System.out.println("Partita terminata. Scrivi exit per uscire.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readBlock(BufferedReader in) throws Exception {
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            if (line.equals("END")) {
                break;
            }
            System.out.println(line);
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    private static boolean isEndGameResponse(String response) {
        return response.contains("Hai perso!")
                || response.contains("Hai vinto!")
                || response.contains("Pareggio!")
                || response.contains("La partita è finita");
    }
}