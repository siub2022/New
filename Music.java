import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class Music {  // Changed from MusicApp to match filename
    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("[DB] PostgreSQL driver registered");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] FATAL: Driver not found!");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv("PORT"));
        String dbUrl = System.getenv("DB_URL");
        
        System.out.println("[APP] Starting on port: " + port);
        System.out.println("[DB] Using URL: " + dbUrl.replaceFirst(":.*@", ":*****@"));

        // Test database connection immediately
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            System.out.println("[DB] Connection test successful");
        } catch (SQLException e) {
            System.err.println("[DB] Connection failed: " + e.getMessage());
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Required endpoints
        server.createContext("/", exchange -> {
            String response = "Music App\nEndpoints:\n- /health\n- /songs";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        
        server.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(200, 2);
            exchange.getResponseBody().write("OK".getBytes());
            exchange.close();
        });
        
        server.createContext("/songs", exchange -> {
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, artist FROM songs")) {
                
                StringBuilder response = new StringBuilder();
                while (rs.next()) {
                    response.append(rs.getString("title"))
                           .append(" | ")  // Better formatting
                           .append(rs.getString("artist"))
                           .append("\n");
                }
                
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.toString().getBytes());
            } catch (Exception e) {
                String error = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("[APP] Server ready on port " + port);
    }
}