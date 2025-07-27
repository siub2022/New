import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    public static void main(String[] args) throws Exception {
        // 1. Get environment variables
        String dbUrl = System.getenv("DB_URL"); // Already includes SSL
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // 2. Start server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 3. Define endpoints
        server.createContext("/", exchange -> {
            String response = "Music22 Server - Endpoints:\n"
                           + "/songs - List all songs\n"
                           + "/add?title=X&singer=Y&link=Z - Add new song";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.createContext("/songs", exchange -> {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, singer, youtubelink FROM songs")) {
                
                StringBuilder response = new StringBuilder();
                while (rs.next()) {
                    response.append(String.format(
                        "Title: %s | Artist: %s | Link: %s\n",
                        rs.getString("title"),
                        rs.getString("singer"),
                        rs.getString("youtubelink")
                    ));
                }
                
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.toString().getBytes());
            } catch (Exception e) {
                sendError(exchange, 500, "Database error: " + e.getMessage());
            }
        });

        // 4. Bonus: Add song endpoint
        server.createContext("/add", exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                String[] params = query.split("&");
                
                String title = URLDecoder.decode(params[0].split("=")[1], "UTF-8");
                String singer = URLDecoder.decode(params[1].split("=")[1], "UTF-8");
                String link = URLDecoder.decode(params[2].split("=")[1], "UTF-8");

                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                     PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO songs(title, singer, youtubelink) VALUES(?,?,?)")) {
                    stmt.setString(1, title);
                    stmt.setString(2, singer);
                    stmt.setString(3, link);
                    stmt.executeUpdate();
                }

                String response = "Added: " + title + " by " + singer;
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
            } catch (Exception e) {
                sendError(exchange, 400, "Invalid request: " + e.getMessage());
            }
        });

        server.start();
        System.out.println("Server running on port " + port);
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        exchange.sendResponseHeaders(code, message.length());
        exchange.getResponseBody().write(message.getBytes());
        exchange.close();
    }
}