package blackjack;

/*
Questo file avvia il server HTTP, serve index.html, gestisce le API e restituisce JSON. 
HttpServer permette proprio di creare context come / o /api/... e associare un handler
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServerMain {
    private static final int PORT = 6767;
    private static BlackjackGame game = new BlackjackGame();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", ServerMain::handleIndex);
        server.createContext("/assets/", ServerMain::handleStaticAssets);
        server.createContext("/api/game/start", exchange -> handleApi(exchange, "start"));
        server.createContext("/api/game/hit", exchange -> handleApi(exchange, "hit"));
        server.createContext("/api/game/stand", exchange -> handleApi(exchange, "stand"));
        server.createContext("/api/game/state", exchange -> handleApi(exchange, "state"));

        server.setExecutor(null);
        server.start();

        System.out.println("Server HTTP avviato su http://localhost:" + PORT);
    }

    private static void handleIndex(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "Metodo non consentito", "text/plain");
            return;
        }

        byte[] fileBytes = Files.readAllBytes(Paths.get("web/index.html"));
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, fileBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }

    private static void handleStaticAssets(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String relativePath = requestPath.replaceFirst("/assets/", "");
        java.nio.file.Path filePath = Paths.get("web/assets", relativePath);

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendResponse(exchange, 404, "File non trovato", "text/plain");
            return;
        }

        String contentType = guessContentType(filePath.toString());
        byte[] fileBytes = Files.readAllBytes(filePath);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, fileBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }

    private static String guessContentType(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";

        return "application/octet-stream";
    }

    private static void handleApi(HttpExchange exchange, String action) throws IOException {
        addCorsHeaders(exchange);

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            GameState state;

            switch (action) {
                case "start":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendResponse(exchange, 405, "{\"error\":\"Metodo non consentito\"}", "application/json");
                        return;
                    }
                    game = new BlackjackGame();
                    state = game.startGame();
                    System.out.println("[SERVER] Nuova partita avviata.");
                    break;

                case "hit":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendResponse(exchange, 405, "{\"error\":\"Metodo non consentito\"}", "application/json");
                        return;
                    }
                    state = game.playerHit();
                    System.out.println("[SERVER] Ricevuto HIT. Totale giocatore: " + state.getPlayerValue());
                    break;

                case "stand":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                        sendResponse(exchange, 405, "{\"error\":\"Metodo non consentito\"}", "application/json");
                        return;
                    }
                    state = game.playerStand();
                    System.out.println("[SERVER] Ricevuto STAND. Partita conclusa: " + state.isGameOver());
                    break;

                case "state":
                    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                        sendResponse(exchange, 405, "{\"error\":\"Metodo non consentito\"}", "application/json");
                        return;
                    }
                    state = game.getState();
                    break;

                default:
                    sendResponse(exchange, 404, "{\"error\":\"Endpoint non trovato\"}", "application/json");
                    return;
            }

            sendResponse(exchange, 200, toJson(state), "application/json");

        } catch (IllegalStateException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Errore interno del server\"}", "application/json");
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String toJson(GameState state) {
        return "{"
                + "\"playerCards\":" + toJsonArray(state.getPlayerCards()) + ","
                + "\"dealerCards\":" + toJsonArray(state.getDealerCards()) + ","
                + "\"dealerVisibleCard\":\"" + escapeJson(state.getDealerVisibleCard()) + "\","
                + "\"playerValue\":" + state.getPlayerValue() + ","
                + "\"dealerValue\":" + state.getDealerValue() + ","
                + "\"gameOver\":" + state.isGameOver() + ","
                + "\"message\":\"" + escapeJson(state.getMessage()) + "\""
                + "}";
    }

    private static String toJsonArray(java.util.List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
            if (i < list.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}