import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class Music {
    static {
        try {
            Class.forName("org.postgresql.Driver");
            DriverManager.registerDriver(new org.postgresql.Driver());
            System.out.println("[DB] Driver double-registered successfully");
        } catch (Exception e) {
            System.err.println("[DB] FATAL: Driver initialization failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        String dbUrl = System.getenv("DB_URL");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "10000"));
        
        // Test database connection first
        try (Connection testConn = DriverManager.getConnection(dbUrl);
             Statement testStmt = testConn.createStatement();
             ResultSet rs = testStmt.executeQuery("SELECT title, singer, youtubelink FROM songs LIMIT 1")) {
            System.out.println("[DB] Connection and query test successful");
        } catch (SQLException e) {
            System.err.println("[DB] Connection test failed:");
            e.printStackTrace();
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", exchange -> {
            String response = "Music2025 API\nEndpoints:\n- /songs\n- /health";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        
        server.createContext("/songs", exchange -> {
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, singer, youtubelink FROM songs")) {
                
                StringBuilder response = new StringBuilder();
                while (rs.next()) {
                    response.append(rs.getString("title"))
                           .append("|")
                           .append(rs.getString("singer"))
                           .append("|")
                           .append(rs.getString("youtubelink"))
                           .append("\n");
                }
                
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.toString().getBytes());
            } catch (Exception e) {
                String error = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
                System.err.println("[ERROR] " + e.getMessage());
            } finally {
                exchange.close();
            }
        });

        server.createContext("/health", exchange -> {
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.start();
        System.out.println("[APP] Server running on port " + port);
    }
}