package blackjack;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private List<String> playerCards = new ArrayList<>();
    private List<String> dealerCards = new ArrayList<>();

    private List<String> playerCardImages = new ArrayList<>();
    private List<String> dealerCardImages = new ArrayList<>();

    private String dealerVisibleCard = "";
    private String dealerVisibleCardImage = "";

    private int playerValue = 0;
    private int dealerValue = 0;
    private boolean gameOver = false;
    private String message = "";

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

    public List<String> getPlayerCardImages() {
        return playerCardImages;
    }

    public void setPlayerCardImages(List<String> playerCardImages) {
        this.playerCardImages = playerCardImages;
    }

    public List<String> getDealerCardImages() {
        return dealerCardImages;
    }

    public void setDealerCardImages(List<String> dealerCardImages) {
        this.dealerCardImages = dealerCardImages;
    }

    public String getDealerVisibleCard() {
        return dealerVisibleCard;
    }

    public void setDealerVisibleCard(String dealerVisibleCard) {
        this.dealerVisibleCard = dealerVisibleCard;
    }

    public String getDealerVisibleCardImage() {
        return dealerVisibleCardImage;
    }

    public void setDealerVisibleCardImage(String dealerVisibleCardImage) {
        this.dealerVisibleCardImage = dealerVisibleCardImage;
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