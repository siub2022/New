import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    static {
        try {
            // 1. REQUIRED: Explicitly load PostgreSQL driver
            Class.forName("org.postgresql.Driver");
            System.out.println("[DB] PostgreSQL driver registered");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] ERROR: PostgreSQL driver not found!");
            System.exit(1);
        }
    }

    private static final String DB_URL = System.getenv("DB_URL");

    public static void main(String[] args) throws Exception {
        // 2. Get port from Render
        int port = Integer.parseInt(System.getenv("PORT"));
        System.out.println("[SERVER] Starting on port " + port);

        // 3. Create server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 4. Setup endpoints
        server.createContext("/", exchange -> {
            String response = "Music22 Server - Use /songs endpoint";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.createContext("/songs", exchange -> {
            try {
                String response = getSongs();
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
            } catch (Exception e) {
                String error = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
                System.err.println("[ERROR] " + error);
            } finally {
                exchange.close();
            }
        });

        // 5. Start server
        server.start();
        System.out.println("[SERVER] Ready at :" + port);
    }

    private static String getSongs() throws SQLException {
        System.out.println("[DB] Connecting to: " + DB_URL);
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
            System.out.println("[DB] Retrieved " + sb.toString().split("\n").length + " songs");
            return sb.toString();
        }
    }
}