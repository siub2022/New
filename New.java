import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class New {
    public static void main(String[] args) throws Exception {
        // Simplified environment variable check
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String port = System.getenv().getOrDefault("PORT", "8080");

        // Basic validation
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Missing required environment variables!");
            System.exit(1);
        }

        // Initialize server
        HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(port)), 0);
        
        // Register endpoints
        server.createContext("/", exchange -> {
            String response = "Music Server - Use /songs endpoint";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        
        server.createContext("/songs", exchange -> {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, singer, youtubelink FROM songs")) {
                
                StringBuilder response = new StringBuilder();
                while (rs.next()) {
                    response.append(rs.getString("title")).append("|")
                           .append(rs.getString("singer")).append("|")
                           .append(rs.getString("youtubelink")).append("\n");
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
        System.out.println("Server running on port " + port);
    }
}