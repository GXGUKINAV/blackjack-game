package blackjack;

public class Main {
    public static void main(String[] args) {
        BlackjackGame game = new BlackjackGame();

        GameState startState = game.startGame();
        printState("START", startState);

        GameState hitState = game.playerHit();
        printState("HIT", hitState);

        if (!hitState.isGameOver()) {
            GameState standState = game.playerStand();
            printState("STAND", standState);
        }
    }

    private static void printState(String action, GameState state) {
        System.out.println("=== " + action + " ===");
        System.out.println("Carte giocatore: " + state.getPlayerCards());
        System.out.println("Valore giocatore: " + state.getPlayerValue());

        if (state.isGameOver()) {
            System.out.println("Carte dealer: " + state.getDealerCards());
            System.out.println("Valore dealer: " + state.getDealerValue());
        } else {
            System.out.println("Carta visibile dealer: " + state.getDealerVisibleCard());
        }

        System.out.println("Messaggio: " + state.getMessage());
        System.out.println("Partita finita: " + state.isGameOver());
        System.out.println();
    }
}
