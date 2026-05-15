package blackjack;

public class BlackjackGame {
    private Deck deck;
    private Hand playerHand;
    private Hand dealerHand;
    private boolean gameOver;

    public BlackjackGame() {
        startNewGame();
    }

    public void startNewGame() {
        deck = new Deck();
        playerHand = new Hand();
        dealerHand = new Hand();
        gameOver = false;

        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());
        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getInitialState() {
        return "Partita iniziata.\n"
                + "Le tue carte: " + playerHand.cardsToString() + " (Totale: " + playerHand.getValue() + ")\n"
                + "Carta visibile dealer: " + dealerHand.getCards().get(0) + "\n"
                + "Scrivi hit o stand.";
    }

    public String handleCommand(String command) {
        if (gameOver) {
            return "La partita è finita. Scrivi exit per uscire.";
        }

        if (command.equalsIgnoreCase("hit")) {
            Card card = deck.drawCard();
            playerHand.addCard(card);

            String response = "Hai pescato: " + card + "\n"
                    + "Le tue carte: " + playerHand.cardsToString() + " (Totale: " + playerHand.getValue() + ")";

            if (playerHand.isBust()) {
                gameOver = true;
                response += "\nHai sballato! Hai perso.";
            } else {
                response += "\nScrivi hit o stand.";
            }

            return response;
        }

        if (command.equalsIgnoreCase("stand")) {
            while (dealerHand.getValue() < 17) {
                dealerHand.addCard(deck.drawCard());
            }

            gameOver = true;

            String response = "Il dealer gioca...\n"
                    + "Dealer: " + dealerHand.cardsToString() + " (Totale: " + dealerHand.getValue() + ")\n"
                    + "Tu: " + playerHand.cardsToString() + " (Totale: " + playerHand.getValue() + ")\n";

            if (dealerHand.isBust()) {
                response += "Il dealer ha sballato. Hai vinto!";
            } else if (playerHand.getValue() > dealerHand.getValue()) {
                response += "Hai vinto!";
            } else if (playerHand.getValue() < dealerHand.getValue()) {
                response += "Hai perso!";
            } else {
                response += "Pareggio!";
            }

            return response;
        }

        return "Comando non valido. Usa hit, stand o exit.";
    }

    public int getPlayerValue() {
        return playerHand.getValue();
    }
}