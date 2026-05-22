package blackjack;

import java.util.List;
import java.util.stream.Collectors;

public class BlackjackGame {
    private Deck   deck;
    private Hand   playerHand;
    private Hand   dealerHand;
    private boolean gameOver;
    private boolean started;
    private String  message;

    // Scommessa corrente e crediti del giocatore
    private double bet       = 0.0;
    private double credits   = 0.0;
    private int    playerId  = -1;  // id reale dal login

    private final DatabaseManager dbManager = new DatabaseManager();

    public BlackjackGame() {
        started  = false;
        gameOver = false;
        message  = "";
    }

    /** Imposta il giocatore (chiamato dal server dopo il login) */
    public void setPlayer(int playerId, double credits) {
        this.playerId = playerId;
        this.credits  = credits;
    }

    public boolean isInProgress() {
        return started && !gameOver;
    }

    /** Imposta la scommessa PRIMA di startGame. Lancia eccezione se non valida. */
    public void setBet(double bet) {
        if (isInProgress()) {
            throw new IllegalStateException("Non puoi cambiare la scommessa durante una partita.");
        }
        if (bet <= 0) {
            throw new IllegalArgumentException("La scommessa deve essere maggiore di 0.");
        }
        if (bet > 100) {
            throw new IllegalArgumentException("La scommessa massima è 100 chips.");
        }
        if (bet > credits) {
            throw new IllegalArgumentException("Crediti insufficienti.");
        }
        this.bet = bet;
    }

    public double getBet()     { return bet; }
    public double getCredits() { return credits; }

    public GameState startGame() {
        if (bet <= 0) {
            throw new IllegalStateException("Imposta una scommessa prima di iniziare.");
        }

        deck       = new Deck();
        playerHand = new Hand();
        dealerHand = new Hand();
        gameOver   = false;
        started    = true;
        message    = "Partita iniziata.";

        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());
        playerHand.addCard(deck.drawCard());
        dealerHand.addCard(deck.drawCard());

        if (playerHand.getValue() == 21) {
            // Blackjack naturale: paga 3:2
            double winnings = bet * 1.5;
            credits += winnings;
            gameOver = true;
            message  = "Blackjack! Hai vinto " + (int) winnings + " chips!";
            saveAndUpdateCredits("WIN", false);
        }

        return buildState(gameOver);
    }

    public GameState playerHit() {
        checkStarted();
        if (gameOver) return buildState(true);

        playerHand.addCard(deck.drawCard());

        if (playerHand.isBust()) {
            credits  -= bet;
            gameOver  = true;
            message   = "Hai sballato! Hai perso " + (int) bet + " chips.";
            saveAndUpdateCredits("LOSE", false);
            return buildState(true);
        }

        message = playerHand.getValue() == 21
                ? "Hai fatto 21. Ora puoi stare."
                : "Hai pescato una carta.";

        return buildState(false);
    }

    public GameState playerStand() {
        checkStarted();
        if (gameOver) return buildState(true);

        while (dealerHand.getValue() < 17) {
            dealerHand.addCard(deck.drawCard());
        }

        gameOver = true;

        int playerValue = playerHand.getValue();
        int dealerValue = dealerHand.getValue();
        String esito;

        if (dealerHand.isBust() || playerValue > dealerValue) {
            credits += bet;
            esito    = "WIN";
            message  = dealerHand.isBust()
                ? "Il dealer ha sballato. Hai vinto " + (int) bet + " chips!"
                : "Hai vinto " + (int) bet + " chips!";
        } else if (playerValue < dealerValue) {
            credits -= bet;
            esito    = "LOSE";
            message  = "Hai perso " + (int) bet + " chips.";
        } else {
            // Pareggio: la scommessa torna indietro (nessuna variazione)
            esito   = "DRAW";
            message = "Pareggio! La scommessa è stata restituita.";
        }

        saveAndUpdateCredits(esito, false);
        return buildState(true);
    }

    public GameState abandonGame() {
        if (!started || gameOver) {
            GameState empty = new GameState();
            empty.setStarted(false);
            empty.setGameOver(true);
            empty.setMessage("Nessuna partita in corso.");
            empty.setCredits(credits);
            return empty;
        }

        while (dealerHand.getValue() < 17) {
            dealerHand.addCard(deck.drawCard());
        }

        credits  -= bet;
        gameOver  = true;
        message   = "Partita abbandonata. Hai perso " + (int) bet + " chips.";

        saveAndUpdateCredits("LOSE", true);
        return buildState(true);
    }

    public GameState getState() {
        if (!started) {
            GameState empty = new GameState();
            empty.setStarted(false);
            empty.setGameOver(false);
            empty.setMessage("Imposta la scommessa e premi Start.");
            empty.setBet(bet);
            empty.setCredits(credits);
            return empty;
        }
        return buildState(gameOver);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void saveAndUpdateCredits(String esito, boolean disconnesso) {
        if (playerId < 0) return;  // sicurezza: non salvare se non loggato
        dbManager.salvaPartita(playerId, esito,
                playerHand.getValue(), dealerHand.getValue(),
                bet, disconnesso);
        dbManager.aggiornaCrediti(playerId, credits);
    }

    private void checkStarted() {
        if (!started) throw new IllegalStateException("La partita non è stata ancora avviata.");
    }

    private GameState buildState(boolean revealDealer) {
        GameState state = new GameState();

        state.setPlayerCards(playerHand.getCards().stream()
                .map(Card::getDisplayName).collect(Collectors.toList()));
        state.setPlayerCardImages(playerHand.getCards().stream()
                .map(Card::getImagePath).collect(Collectors.toList()));
        state.setPlayerValue(playerHand.getValue());
        state.setGameOver(gameOver);
        state.setStarted(started);
        state.setMessage(message);
        state.setBet(bet);
        state.setCredits(credits);

        if (revealDealer) {
            state.setDealerCards(dealerHand.getCards().stream()
                    .map(Card::getDisplayName).collect(Collectors.toList()));
            state.setDealerCardImages(dealerHand.getCards().stream()
                    .map(Card::getImagePath).collect(Collectors.toList()));
            state.setDealerValue(dealerHand.getValue());
        } else {
            state.setDealerCards(List.of("Carta coperta"));
            state.setDealerCardImages(List.of("/assets/back_side.png"));
            state.setDealerValue(0);
        }

        return state;
    }
}