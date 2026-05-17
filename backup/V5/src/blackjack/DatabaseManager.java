package blackjack;

import java.sql.*;

public class DatabaseManager {

    private static final String URL      = "jdbc:mysql://localhost:3306/blackjack_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "";   // XAMPP default: nessuna password

    private Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver NON trovato", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Salva una partita appena conclusa.
     * Per adesso il giocatore ha sempre id=1 (utente singolo).
     * Quando farai login, passerai l'id reale del giocatore.
     *
     * @param esitoGiocatore  "WIN", "LOSE" o "DRAW" dal punto di vista del giocatore
     * @param puntiGiocatore  valore finale della mano del giocatore
     * @param puntiDealer     valore finale della mano del dealer
     */
    public void salvaPartita(String esitoGiocatore, int puntiGiocatore, int puntiDealer, boolean playerDisconnesso) {
        String esitoDealer = calcolaEsitoDealer(esitoGiocatore);

        String insertPartita = "INSERT INTO Partita (data_ora) VALUES (NOW())";
        String insertPartecipazione =
        "INSERT INTO PartecipazionePartita " +
        "(fk_giocatore, fk_partita, esito, numero_realizzato, somma_scommessa, player_disconnesso) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);  // transazione atomica

            // 1. Inserisci la partita e recupera l'id generato
            int idPartita;
            try (PreparedStatement ps = conn.prepareStatement(
                    insertPartita, Statement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    idPartita = rs.getInt(1);
                }
            }

            // 2. Riga del giocatore (id=1 per ora, somma_scommessa=0 finché non fai scommesse)
            try (PreparedStatement ps = conn.prepareStatement(insertPartecipazione)) {
                ps.setInt(1, 1);
                ps.setInt(2, idPartita);
                ps.setString(3, esitoGiocatore);
                ps.setInt(4, puntiGiocatore);
                ps.setDouble(5, 0.00);
                ps.setBoolean(6, playerDisconnesso);
                ps.executeUpdate();
            }

            // 3. Riga del dealer (id=0)
            try (PreparedStatement ps = conn.prepareStatement(insertPartecipazione)) {
                ps.setInt(1, 0);                    // fk_giocatore = Dealer
                ps.setInt(2, idPartita);
                ps.setString(3, esitoDealer);
                ps.setInt(4, puntiDealer);
                ps.setDouble(5, 0.00);
                ps.setBoolean(6, false);  // player_disconnesso = false per il dealer
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[DB] Partita salvata — id=" + idPartita +
                               " | giocatore=" + esitoGiocatore +
                               " (pts=" + puntiGiocatore + ")" +
                               " | dealer=" + esitoDealer +
                               " (pts=" + puntiDealer + ")");

        } catch (SQLException e) {
            System.err.println("[DB] Errore salvataggio partita: " + e.getMessage());
        }
    }

    /** Deriva l'esito del dealer dall'esito opposto del giocatore */
    private String calcolaEsitoDealer(String esitoGiocatore) {
        switch (esitoGiocatore) {
            case "WIN":  return "LOSE";
            case "LOSE": return "WIN";
            default:     return "DRAW";
        }
    }
}
