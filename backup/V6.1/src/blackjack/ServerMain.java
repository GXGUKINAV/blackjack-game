package blackjack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {
    private static final int PORT = 6767;

    // ── Sessioni attive: token → PlayerSession ──────────────────────────────
    private static final Map<String, PlayerSession> sessions = new ConcurrentHashMap<>();

    // ── Partita per sessione: token → BlackjackGame ─────────────────────────
    private static final Map<String, BlackjackGame> games = new ConcurrentHashMap<>();

    private static final DatabaseManager db = new DatabaseManager();

    // Dati della sessione utente
    private static class PlayerSession {
        int    playerId;
        @SuppressWarnings("unused")  
        String username;
        double credits;

        PlayerSession(int playerId, String username, double credits) {
            this.playerId = playerId;
            this.username = username;
            this.credits  = credits;
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Pagina e asset statici
        server.createContext("/",        ServerMain::handleIndex);
        server.createContext("/style.css", ServerMain::handleStyle);  // Context per il file CSS
        server.createContext("/blackjack.js", ServerMain::handleJS);  // Context per il file JavaScript
        server.createContext("/assets/", ServerMain::handleStaticAssets);
        server.createContext("/logo/",   ServerMain::handleStaticLogo);

        // Auth
        server.createContext("/api/auth/login",    exchange -> handleAuth(exchange, "login"));
        server.createContext("/api/auth/register", exchange -> handleAuth(exchange, "register"));
        server.createContext("/api/auth/logout",   exchange -> handleAuth(exchange, "logout"));

        // Gioco (richiedono sessione valida)
        server.createContext("/api/game/start",   exchange -> handleGame(exchange, "start"));
        server.createContext("/api/game/hit",     exchange -> handleGame(exchange, "hit"));
        server.createContext("/api/game/stand",   exchange -> handleGame(exchange, "stand"));
        server.createContext("/api/game/state",   exchange -> handleGame(exchange, "state"));
        server.createContext("/api/game/abandon", exchange -> handleGame(exchange, "abandon"));
        server.createContext("/api/game/bet",     exchange -> handleGame(exchange, "bet"));

        // Statistiche
        server.createContext("/api/stats", ServerMain::handleStats);

        server.setExecutor(null);
        server.start();

        String ip = java.net.InetAddress.getLocalHost().getHostAddress();
        System.out.println("Server HTTP avviato su http://" + ip + ":" + PORT);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleAuth(HttpExchange exchange, String action) throws IOException {
        addCorsHeaders(exchange);
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (action) {

                case "login": {
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405, "{\"error\":\"Metodo non consentito\"}");
                        return;
                    }
                    String body = readBody(exchange);
                    String username = extractField(body, "username");
                    String password = extractField(body, "password");

                    int id = db.login(username, password);
                    if (id < 0) {
                        sendJson(exchange, 401, "{\"error\":\"Credenziali non valide\"}");
                        return;
                    }

                    double credits = db.getCrediti(id);
                    String token   = UUID.randomUUID().toString();
                    sessions.put(token, new PlayerSession(id, username, credits));
                    games.put(token, createGame(id, credits));

                    System.out.println("[AUTH] Login OK — " + username + " token=" + token);
                    sendJson(exchange, 200,
                        "{\"token\":\"" + token + "\","
                        + "\"username\":\"" + escapeJson(username) + "\","
                        + "\"credits\":" + credits + "}");
                    break;
                }

                case "register": {
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405, "{\"error\":\"Metodo non consentito\"}");
                        return;
                    }
                    String body     = readBody(exchange);
                    String username = extractField(body, "username");
                    String password = extractField(body, "password");

                    if (username.isEmpty() || password.isEmpty()) {
                        sendJson(exchange, 400, "{\"error\":\"Username e password obbligatori\"}");
                        return;
                    }

                    int id = db.register(username, password);
                    if (id < 0) {
                        sendJson(exchange, 409, "{\"error\":\"Username già in uso\"}");
                        return;
                    }

                    double credits = 1000.0;
                    String token   = UUID.randomUUID().toString();
                    sessions.put(token, new PlayerSession(id, username, credits));
                    games.put(token, createGame(id, credits));

                    System.out.println("[AUTH] Registrazione OK — " + username);
                    sendJson(exchange, 200,
                        "{\"token\":\"" + token + "\","
                        + "\"username\":\"" + escapeJson(username) + "\","
                        + "\"credits\":" + credits + "}");
                    break;
                }

                case "logout": {
                    String token = getToken(exchange);
                    if (token != null) {
                        // Se c'è una partita in corso, la abbandoniamo
                        BlackjackGame game = games.get(token);
                        if (game != null && game.isInProgress()) {
                            game.abandonGame();
                        }
                        sessions.remove(token);
                        games.remove(token);
                        System.out.println("[AUTH] Logout — token=" + token);
                    }
                    sendJson(exchange, 200, "{\"ok\":true}");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Errore interno\"}");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GAME
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleGame(HttpExchange exchange, String action) throws IOException {
        addCorsHeaders(exchange);
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // abandon accetta qualsiasi metodo (sendBeacon usa POST senza header custom)
        String token = getToken(exchange);
        if (!action.equals("abandon") && (token == null || !sessions.containsKey(token))) {
            sendJson(exchange, 401, "{\"error\":\"Non autenticato\"}");
            return;
        }

        // Per abandon: se il token non esiste non facciamo nulla
        if (action.equals("abandon") && (token == null || !sessions.containsKey(token))) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            BlackjackGame game    = games.get(token);
            PlayerSession session = sessions.get(token);
            GameState     state;

            switch (action) {

                case "start":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405, "{\"error\":\"Metodo non consentito\"}"); return;
                    }
                    if (game.isInProgress()) {
                        sendJson(exchange, 409, "{\"error\":\"Partita già in corso\"}"); return;
                    }
                    // Nuova istanza con i dati aggiornati dal DB
                    session.credits = db.getCrediti(session.playerId);
                    game = createGame(session.playerId, session.credits);
                    games.put(token, game);

                    // Leggi la scommessa dal body
                    String startBody = readBody(exchange);
                    String betStr    = extractField(startBody, "bet");
                    if (betStr.isEmpty()) {
                        sendJson(exchange, 400, "{\"error\":\"Scommessa mancante\"}"); return;
                    }
                    double betVal = Double.parseDouble(betStr);
                    game.setBet(betVal);

                    state = game.startGame();
                    session.credits = game.getCredits();
                    System.out.println("[SERVER] Nuova partita — bet=" + betVal);
                    break;

                case "bet":
                    // Permette di aggiornare la scommessa fuori dalla partita
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405, "{\"error\":\"Metodo non consentito\"}"); return;
                    }
                    String betBody  = readBody(exchange);
                    String betValue = extractField(betBody, "bet");
                    game.setBet(Double.parseDouble(betValue));
                    state = game.getState();
                    break;

                case "hit":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405, "{\"error\":\"Metodo non consentito\"}"); return;
                    }
                    state = game.playerHit();
                    session.credits = game.getCredits();
                    System.out.println("[SERVER] HIT — player=" + state.getPlayerValue());
                    break;

                case "stand":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendJson(exchange, 405, "{\"error\":\"Metodo non consentito\"}"); return;
                    }
                    state = game.playerStand();
                    session.credits = game.getCredits();
                    System.out.println("[SERVER] STAND — gameOver=" + state.isGameOver());
                    break;

                case "state":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                        sendJson(exchange, 405, "{\"error\":\"Metodo non consentito\"}"); return;
                    }
                    state = game.getState();
                    break;

                case "abandon":
                    state = game.abandonGame();
                    if (session != null) session.credits = game.getCredits();
                    System.out.println("[SERVER] Partita abbandonata.");
                    break;

                default:
                    sendJson(exchange, 404, "{\"error\":\"Endpoint non trovato\"}"); return;
            }

            sendJson(exchange, 200, toJson(state));

        } catch (NumberFormatException e) {
            sendJson(exchange, 400, "{\"error\":\"Valore scommessa non valido\"}");
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Errore interno del server\"}");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleStats(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String token = getToken(exchange);
        if (token == null || !sessions.containsKey(token)) {
            sendJson(exchange, 401, "{\"error\":\"Non autenticato\"}");
            return;
        }

        PlayerSession session = sessions.get(token);
        String json = db.getStatistiche(session.playerId);
        sendJson(exchange, 200, json);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATIC FILE HANDLERS
    // ─────────────────────────────────────────────────────────────────────────

    private static void handleIndex(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Metodo non consentito", "text/plain");
            return;
        }
        byte[] fileBytes = Files.readAllBytes(Paths.get("Blackjack/web/index.html"));
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, fileBytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(fileBytes); }
    }

    private static void handleStyle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Metodo non consentito", "text/plain");
            return;
        }
        byte[] fileBytes = Files.readAllBytes(Paths.get("Blackjack/web/style.css"));
        exchange.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
        exchange.sendResponseHeaders(200, fileBytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(fileBytes); }
    }

    private static void handleJS(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Metodo non consentito", "text/plain");
            return;
        }
        byte[] fileBytes = Files.readAllBytes(Paths.get("Blackjack/web/blackjack.js"));
        exchange.getResponseHeaders().set("Content-Type", "application/javascript; charset=UTF-8");
        exchange.sendResponseHeaders(200, fileBytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(fileBytes); }
    }

    private static void handleStaticAssets(HttpExchange exchange) throws IOException {
        serveStaticFile(exchange,
            exchange.getRequestURI().getPath().replaceFirst("/assets/", ""),
            "Blackjack/web/assets");
    }

    private static void handleStaticLogo(HttpExchange exchange) throws IOException {
        serveStaticFile(exchange,
            exchange.getRequestURI().getPath().replaceFirst("/logo/", ""),
            "Blackjack/web/logo");
    }

    private static void serveStaticFile(HttpExchange exchange, String relative, String base) throws IOException {
        java.nio.file.Path filePath = Paths.get(base, relative);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendResponse(exchange, 404, "File non trovato", "text/plain"); return;
        }
        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", guessContentType(filePath.toString()));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITY
    // ─────────────────────────────────────────────────────────────────────────

    private static BlackjackGame createGame(int playerId, double credits) {
        BlackjackGame g = new BlackjackGame();
        g.setPlayer(playerId, credits);
        return g;
    }

    /** Estrae il token dall'header Authorization: Bearer <token> */
    private static String getToken(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        return null;
    }

    /** Legge il body come stringa UTF-8 */
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Estrae un campo da JSON minimale: {"field":"value"} o {"field":123}
     * Solo per campi semplici (stringhe senza escape complesso, numeri).
     */
    private static String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return "";

        if (json.charAt(start) == '"') {
            // Valore stringa
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        } else {
            // Valore numerico o booleano
            int end = start;
            while (end < json.length() && ",}]".indexOf(json.charAt(end)) < 0) end++;
            return json.substring(start, end).trim();
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        sendResponse(exchange, code, json, "application/json");
    }

    private static void sendResponse(HttpExchange exchange, int code, String body, String ct) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", ct + "; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static String toJson(GameState state) {
        return "{"
            + "\"playerCards\":"      + toJsonArray(state.getPlayerCards())      + ","
            + "\"dealerCards\":"      + toJsonArray(state.getDealerCards())      + ","
            + "\"playerCardImages\":" + toJsonArray(state.getPlayerCardImages()) + ","
            + "\"dealerCardImages\":" + toJsonArray(state.getDealerCardImages()) + ","
            + "\"playerValue\":"      + state.getPlayerValue()                   + ","
            + "\"dealerValue\":"      + state.getDealerValue()                   + ","
            + "\"gameOver\":"         + state.isGameOver()                       + ","
            + "\"started\":"          + state.isStarted()                        + ","
            + "\"bet\":"              + state.getBet()                           + ","
            + "\"credits\":"          + state.getCredits()                       + ","
            + "\"message\":\""        + escapeJson(state.getMessage())           + "\""
            + "}";
    }

    private static String toJsonArray(java.util.List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String guessContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (lower.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}