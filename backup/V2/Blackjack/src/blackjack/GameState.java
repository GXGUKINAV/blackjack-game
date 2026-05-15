package blackjack;

//Questa classe serve per rappresentare lo stato della partita che poi il server manderà al client. 
//Nei sistemi client-server turn-based, il server conserva lo stato e lo restituisce ai client come risposta ai loro comandi.

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private List<String> playerCards;
    private List<String> dealerCards;
    private String dealerVisibleCard;
    private int playerValue;
    private int dealerValue;
    private boolean gameOver;
    private String message;

    public GameState() {
        this.playerCards = new ArrayList<>();
        this.dealerCards = new ArrayList<>();
        this.dealerVisibleCard = "";
        this.playerValue = 0;
        this.dealerValue = 0;
        this.gameOver = false;
        this.message = "";
    }

    public List<String> getPlayerCards() {
        return playerCards;
    }

    public void setPlayerCards(List<String> playerCards) {
        this.playerCards = playerCards;
    }

    public List<String> getDealerCards() {
        return dealerCards;
    }

    public void setDealerCards(List<String> dealerCards) {
        this.dealerCards = dealerCards;
    }

    public String getDealerVisibleCard() {
        return dealerVisibleCard;
    }

    public void setDealerVisibleCard(String dealerVisibleCard) {
        this.dealerVisibleCard = dealerVisibleCard;
    }

    public int getPlayerValue() {
        return playerValue;
    }

    public void setPlayerValue(int playerValue) {
        this.playerValue = playerValue;
    }

    public int getDealerValue() {
        return dealerValue;
    }

    public void setDealerValue(int dealerValue) {
        this.dealerValue = dealerValue;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}