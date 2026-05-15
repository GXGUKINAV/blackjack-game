package blackjack;

import java.util.Scanner;

public class BlackjackGame {
    private final Deck deck;
    private final Hand playerHand;
    private final Hand dealerHand;
    private final Scanner scanner;

    public BlackjackGame() {
        deck = new Deck();
        playerHand = new Hand();
        dealerHand = new Hand();
        scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== BLACKJACK ===");

        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());
        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());

        playerTurn();

        if (!playerHand.isBust()) {
            dealerTurn();
            showResult();
        }
    }

    private void playerTurn() {
        while (true) {
            System.out.println();
            System.out.println("Le tue carte: " + playerHand);
            System.out.println("Carta visibile del dealer: " + dealerHand.getCards().get(0));

            if (playerHand.getValue() == 21) {
                System.out.println("Blackjack o 21! Hai fermato il turno.");
                return;
            }

            System.out.print("Vuoi pescare o stare? (hit/stand): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("hit")) {
                Card newCard = deck.drawCard();
                playerHand.addCard(newCard);
                System.out.println("Hai pescato: " + newCard);

                if (playerHand.isBust()) {
                    System.out.println("Le tue carte: " + playerHand);
                    System.out.println("Hai sballato! Hai perso.");
                    return;
                }
            } else if (choice.equals("stand")) {
                return;
            } else {
                System.out.println("Scelta non valida. Scrivi hit oppure stand.");
            }
        }
    }

    private void dealerTurn() {
        System.out.println();
        System.out.println("=== TURNO DEL DEALER ===");
        System.out.println("Carte dealer: " + dealerHand);

        while (dealerHand.getValue() < 17) {
            Card newCard = deck.drawCard();
            dealerHand.addCard(newCard);
            System.out.println("Il dealer pesca: " + newCard);
            System.out.println("Carte dealer: " + dealerHand);
        }
    }

    private void showResult() {
        System.out.println();
        System.out.println("=== RISULTATO FINALE ===");
        System.out.println("Le tue carte: " + playerHand);
        System.out.println("Carte dealer: " + dealerHand);

        int playerValue = playerHand.getValue();
        int dealerValue = dealerHand.getValue();

        if (dealerHand.isBust()) {
            System.out.println("Il dealer ha sballato. Hai vinto!");
        } else if (playerValue > dealerValue) {
            System.out.println("Hai vinto!");
        } else if (playerValue < dealerValue) {
            System.out.println("Hai perso!");
        } else {
            System.out.println("Pareggio!");
        }
    }
}