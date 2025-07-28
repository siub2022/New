import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    private static final String DB_URL = System.getenv("DB_URL");

    public static void main(String[] args) throws Exception {
        // 1. Get port from Render (required)
        int port = Integer.parseInt(System.getenv("PORT"));
        System.out.println("[SERVER] Starting on port " + port);

        // 2. Create server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 3. Setup endpoints
        server.createContext("/", exchange -> {
            String response = "Music22 Server - Use /songs endpoint";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.createContext("/songs", exchange -> {
            try {
                String response = getSongs();
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
            } catch (Exception e) {
                String error = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        });

        // 4. Start server
        server.start();
        System.out.println("[SERVER] Ready at http://localhost:" + port);
    }

    private static String getSongs() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT title, singer, youtubelink FROM songs")) {

            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(String.format("%s|%s|%s\n",
                    rs.getString("title"),
                    rs.getString("singer"),
                    rs.getString("youtubelink")));
            }
            return sb.toString();
        }
    }
}