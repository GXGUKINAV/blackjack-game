package blackjack;

//logica del gioco

import java.util.List;
import java.util.stream.Collectors;

public class BlackjackGame {
    private Deck deck;
    private Hand playerHand;
    private Hand dealerHand;
    private boolean gameOver;
    private boolean started;
    private String message;

    public BlackjackGame() {
        started = false;
        gameOver = false;
        message = "";
    }

    public GameState startGame() {
        deck = new Deck();
        playerHand = new Hand();
        dealerHand = new Hand();
        gameOver = false;
        started = true;
        message = "Partita iniziata.";

        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());
        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());

        if (playerHand.getValue() == 21) {
            gameOver = true;
            message = "Blackjack! Hai vinto!";
        }

        return buildState(false);
    }

    public GameState playerHit() {
        checkStarted();

        if (gameOver) {
            return buildState(true);
        }

        playerHand.addCard(deck.drawCard());

        if (playerHand.isBust()) {
            gameOver = true;
            message = "Hai sballato! Hai perso.";
            return buildState(true);
        }

        if (playerHand.getValue() == 21) {
            message = "Hai fatto 21. Ora puoi stare.";
        } else {
            message = "Hai pescato una carta.";
        }

        return buildState(false);
    }

    public GameState playerStand() {
        checkStarted();

        if (gameOver) {
            return buildState(true);
        }

        while (dealerHand.getValue() < 17) {
            dealerHand.addCard(deck.drawCard());
        }

        gameOver = true;

        int playerValue = playerHand.getValue();
        int dealerValue = dealerHand.getValue();

        if (dealerHand.isBust()) {
            message = "Il dealer ha sballato. Hai vinto!";
        } else if (playerValue > dealerValue) {
            message = "Hai vinto!";
        } else if (playerValue < dealerValue) {
            message = "Hai perso!";
        } else {
            message = "Pareggio!";
        }

        return buildState(true);
    }

    public GameState getState() {
        checkStarted();
        return buildState(gameOver);
    }

    private void checkStarted() {
        if (!started) {
            throw new IllegalStateException("La partita non è stata ancora avviata.");
        }
    }

    private GameState buildState(boolean revealDealer) {
        GameState state = new GameState();

        List<String> playerCards = playerHand.getCards()
                .stream()
                .map(Card::getDisplayName)
                .collect(Collectors.toList());

        List<String> playerCardImages = playerHand.getCards()
                .stream()
                .map(Card::getImagePath)
                .collect(Collectors.toList());

        state.setPlayerCards(playerCards);
        state.setPlayerCardImages(playerCardImages);
        state.setPlayerValue(playerHand.getValue());
        state.setGameOver(gameOver);
        state.setMessage(message);

        if (revealDealer) {
            List<String> dealerCards = dealerHand.getCards()
                    .stream()
                    .map(Card::getDisplayName)
                    .collect(Collectors.toList());

            List<String> dealerCardImages = dealerHand.getCards()
                    .stream()
                    .map(Card::getImagePath)
                    .collect(Collectors.toList());

            state.setDealerCards(dealerCards);
            state.setDealerCardImages(dealerCardImages);
            state.setDealerValue(dealerHand.getValue());
            state.setDealerVisibleCard("");
            state.setDealerVisibleCardImage("");
        } else {
            Card visibleDealerCard = dealerHand.getCards().get(0);

            state.setDealerCards(List.of("Carta coperta"));
            state.setDealerCardImages(List.of("/assets/cards/back.png"));
            state.setDealerValue(0);

            state.setDealerVisibleCard(visibleDealerCard.getDisplayName());
            state.setDealerVisibleCardImage(visibleDealerCard.getImagePath());
        }

        return state;
    }
}