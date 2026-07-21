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

    // ─────────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica le credenziali di login.
     * @return id del giocatore se ok, -1 se username/password errati
     */
    public int login(String username, String password) {
        String sql = "SELECT id FROM Giocatore WHERE username = ? AND password = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    System.out.println("[DB] Login OK — id=" + id + " username=" + username);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore login: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Registra un nuovo giocatore con 1000 crediti iniziali.
     * @return id generato, oppure -1 se username già esistente o errore
     */
    public int register(String username, String password) {
        String sql = "INSERT INTO Giocatore (username, password, crediti) VALUES (?, ?, 1000.00)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("[DB] Registrazione OK — id=" + id + " username=" + username);
                    return id;
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.err.println("[DB] Username già esistente: " + username);
        } catch (SQLException e) {
            System.err.println("[DB] Errore registrazione: " + e.getMessage());
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREDITI
    // ─────────────────────────────────────────────────────────────────────────

    /** Restituisce i crediti attuali del giocatore */
    public double getCrediti(int playerId) {
        String sql = "SELECT crediti FROM Giocatore WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("crediti");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Errore getCrediti: " + e.getMessage());
        }
        return 0.0;
    }

    /** Aggiorna i crediti del giocatore (può essere positivo o negativo il delta) */
    public void aggiornaCrediti(int playerId, double nuoviCrediti) {
        String sql = "UPDATE Giocatore SET crediti = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nuoviCrediti);
            ps.setInt(2, playerId);
            ps.executeUpdate();
            System.out.println("[DB] Crediti aggiornati — id=" + playerId + " crediti=" + nuoviCrediti);
        } catch (SQLException e) {
            System.err.println("[DB] Errore aggiornaCrediti: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATISTICHE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Restituisce le statistiche del giocatore come stringa JSON.
     * Campi: totalPartite, vittorie, sconfitte, pareggi, creditiAttuali
     */
    public String getStatistiche(int playerId) {
        String sql =
            "SELECT " +
            "  COUNT(*) AS totale, " +
            "  SUM(CASE WHEN esito='WIN'  THEN 1 ELSE 0 END) AS vittorie, " +
            "  SUM(CASE WHEN esito='LOSE' THEN 1 ELSE 0 END) AS sconfitte, " +
            "  SUM(CASE WHEN esito='DRAW' THEN 1 ELSE 0 END) AS pareggi, " +
            "  SUM(somma_scommessa) AS totale_scommesso " +
            "FROM PartecipazionePartita " +
            "WHERE fk_giocatore = ?";

        String sqlCrediti = "SELECT username, crediti FROM Giocatore WHERE id = ?";

        try (Connection conn = connect()) {
            int totale = 0, vittorie = 0, sconfitte = 0, pareggi = 0;
            double totaleScommesso = 0.0;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totale          = rs.getInt("totale");
                        vittorie        = rs.getInt("vittorie");
                        sconfitte       = rs.getInt("sconfitte");
                        pareggi         = rs.getInt("pareggi");
                        totaleScommesso = rs.getDouble("totale_scommesso");
                    }
                }
            }

            String username = "";
            double crediti  = 0.0;
            try (PreparedStatement ps = conn.prepareStatement(sqlCrediti)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        username = rs.getString("username");
                        crediti  = rs.getDouble("crediti");
                    }
                }
            }

            // Ultime 10 partite
            String sqlUltime =
                "SELECT p.data_ora, pp.esito, pp.numero_realizzato, pp.somma_scommessa " +
                "FROM PartecipazionePartita pp " +
                "JOIN Partita p ON p.id = pp.fk_partita " +
                "WHERE pp.fk_giocatore = ? " +
                "ORDER BY p.data_ora DESC LIMIT 10";

            StringBuilder ultimeJson = new StringBuilder("[");
            try (PreparedStatement ps = conn.prepareStatement(sqlUltime)) {
                ps.setInt(1, playerId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) ultimeJson.append(",");
                        ultimeJson.append("{")
                            .append("\"data\":\"").append(rs.getString("data_ora")).append("\",")
                            .append("\"esito\":\"").append(rs.getString("esito")).append("\",")
                            .append("\"punti\":").append(rs.getInt("numero_realizzato")).append(",")
                            .append("\"scommessa\":").append(rs.getDouble("somma_scommessa"))
                            .append("}");
                        first = false;
                    }
                }
            }
            ultimeJson.append("]");

            return "{"
                + "\"username\":\"" + username + "\","
                + "\"crediti\":"    + crediti  + ","
                + "\"totale\":"     + totale   + ","
                + "\"vittorie\":"   + vittorie + ","
                + "\"sconfitte\":"  + sconfitte + ","
                + "\"pareggi\":"    + pareggi  + ","
                + "\"totaleScommesso\":" + totaleScommesso + ","
                + "\"ultimePartite\":" + ultimeJson
                + "}";

        } catch (SQLException e) {
            System.err.println("[DB] Errore statistiche: " + e.getMessage());
            return "{\"error\":\"Errore DB\"}";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARTITA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Salva una partita appena conclusa.
     *
     * @param playerId         id reale del giocatore (dal login)
     * @param esitoGiocatore   "WIN", "LOSE" o "DRAW" dal punto di vista del giocatore
     * @param puntiGiocatore   valore finale della mano del giocatore
     * @param puntiDealer      valore finale della mano del dealer
     * @param scommessa        chips scommesse in questa partita
     * @param playerDisconnesso true se la partita è stata abbandonata
     */
    public void salvaPartita(int playerId, String esitoGiocatore, int puntiGiocatore,
                             int puntiDealer, double scommessa, boolean playerDisconnesso) {
        String esitoDealer = calcolaEsitoDealer(esitoGiocatore);

        String insertPartita = "INSERT INTO Partita (data_ora) VALUES (NOW())";
        String insertPartecipazione =
            "INSERT INTO PartecipazionePartita " +
            "(fk_giocatore, fk_partita, esito, numero_realizzato, somma_scommessa, player_disconnesso) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect()) {
            conn.setAutoCommit(false);

            int idPartita;
            try (PreparedStatement ps = conn.prepareStatement(
                    insertPartita, Statement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    idPartita = rs.getInt(1);
                }
            }

            // Riga del giocatore
            try (PreparedStatement ps = conn.prepareStatement(insertPartecipazione)) {
                ps.setInt(1, playerId);
                ps.setInt(2, idPartita);
                ps.setString(3, esitoGiocatore);
                ps.setInt(4, puntiGiocatore);
                ps.setDouble(5, scommessa);
                ps.setBoolean(6, playerDisconnesso);
                ps.executeUpdate();
            }

            // Riga del dealer (id=0)
            try (PreparedStatement ps = conn.prepareStatement(insertPartecipazione)) {
                ps.setInt(1, 0);
                ps.setInt(2, idPartita);
                ps.setString(3, esitoDealer);
                ps.setInt(4, puntiDealer);
                ps.setDouble(5, 0.00);
                ps.setBoolean(6, false);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("[DB] Partita salvata — id=" + idPartita
                + " | player(id=" + playerId + ")=" + esitoGiocatore
                + " (pts=" + puntiGiocatore + ", bet=" + scommessa + ")"
                + " | dealer=" + esitoDealer + " (pts=" + puntiDealer + ")");

        } catch (SQLException e) {
            System.err.println("[DB] Errore salvataggio partita: " + e.getMessage());
        }
    }

    private String calcolaEsitoDealer(String esitoGiocatore) {
        switch (esitoGiocatore) {
            case "WIN":  return "LOSE";
            case "LOSE": return "WIN";
            default:     return "DRAW";
        }
    }
}