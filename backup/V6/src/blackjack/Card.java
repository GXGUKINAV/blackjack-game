package blackjack;

public class Card {
    private final String suit;
    private final String rank;

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }

    public int getValue() {
        switch (rank) {
            case "A":
                return 11;
            case "K":
            case "Q":
            case "J":
                return 10;
            default:
                return Integer.parseInt(rank);
        }
    }

    public String getSuitForAsset() {
        switch (suit) {
            case "Picche": return "spades";
            case "Cuori": return "hearts";
            case "Quadri": return "diamonds";
            case "Fiori": return "clubs";
            default: throw new IllegalStateException("Seme non valido: " + suit);
        }
    }

    public String getRankForAsset() {
        switch (rank) {
            case "A": return "ace";
            case "K": return "king";
            case "Q": return "queen";
            case "J": return "jack";
            default: return rank;
        }
    }

    public String getAssetFileName() {
        String assetRank = getRankForAsset();
        String assetSuit = getSuitForAsset();

        if ("ace".equals(assetRank) && "spades".equals(assetSuit)) {
            return "ace_of_spades";
        }

        if ("jack".equals(assetRank) || "queen".equals(assetRank) || "king".equals(assetRank)) {
            return assetRank + "_of_" + assetSuit + "2";
        }

        return assetRank + "_of_" + assetSuit;
    }

    public String getImagePath() {
        return "/assets/cards/" + getAssetFileName() + ".png";
    }

    public String getDisplayName() {
        return getRankForAsset() + " of " + getSuitForAsset();
    }

    @Override
    public String toString() {
        return rank + " di " + suit;
    }
}