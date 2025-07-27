import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    public static void main(String[] args) throws Exception {
        // 1. Get credentials from Render environment variables (SECURE)
        String dbUrl = System.getenv("DB_URL") + "?ssl=true&sslmode=require";
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // 2. Modern JDBC automatically loads driver - no Class.forName() needed
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 3. Simplified endpoints
        server.createContext("/", exchange -> {
            String response = "Music22 Server - Use /songs endpoint";
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
        while (true) Thread.sleep(1000); // Keep alive
    }
}