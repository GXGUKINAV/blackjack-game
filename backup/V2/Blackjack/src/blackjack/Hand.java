package blackjack;

import java.util.ArrayList;
import java.util.List;

public class Hand {
    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
    }

    public List<Card> getCards() {
        return cards;
    }

    public int getValue() {
        int total = 0;
        int aces = 0;

        for (Card card : cards) {
            total += card.getValue();
            if (card.getRank().equals("A")) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    public boolean isBust() {
        return getValue() > 21;
    }

    public String cardsToString() {
        StringBuilder sb = new StringBuilder();
        for (Card c : cards) {
            sb.append(c).append(", ");
        }
        if (!cards.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}