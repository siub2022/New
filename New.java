import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    public static void main(String[] args) throws Exception {
        // 1. Get credentials from environment
        String dbUrl = System.getenv("DB_URL"); // SSL params already included
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // Debug: Print connection details (check Render logs)
        System.out.println("DB Connection Details:");
        System.out.println("URL: " + dbUrl.replace(dbPassword, "******"));
        System.out.println("User: " + dbUser);

        // 2. Start server
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", exchange -> {
            String response = "Music22 Server - Use /songs endpoint";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });

        server.createContext("/songs", exchange -> {
            try {
                System.out.println("Attempting database connection...");
                
                // 3. Connect to DB
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT title, singer, youtubelink FROM songs")) {
                    
                    System.out.println("Database connection successful!");
                    
                    // 4. Build response
                    StringBuilder response = new StringBuilder();
                    while (rs.next()) {
                        response.append(rs.getString("title")).append("|")
                               .append(rs.getString("singer")).append("|")
                               .append(rs.getString("youtubelink")).append("\n");
                    }
                    
                    // 5. Send response
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.toString().getBytes());
                    
                    System.out.println("Sent " + response.length() + " bytes of song data");
                }
            } catch (SQLException e) {
                // Detailed error logging
                System.err.println("Database error:");
                e.printStackTrace();
                
                String error = "Database Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("Server running on port " + port);
        while (true) Thread.sleep(1000);
    }
}