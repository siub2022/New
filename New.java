import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    // Static block to FORCE driver registration
    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("[INIT] PostgreSQL driver registered");
        } catch (ClassNotFoundException e) {
            System.err.println("[FATAL] PostgreSQL driver missing!");
            System.exit(1);
        }
    }

    private static final String DB_URL = System.getenv("DB_URL");

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv("PORT"));
        System.out.println("[START] Server starting on port " + port);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/songs", exchange -> {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, singer FROM songs")) {
                
                StringBuilder response = new StringBuilder();
                while (rs.next()) {
                    response.append(rs.getString("title"))
                           .append("|")
                           .append(rs.getString("singer"))
                           .append("\n");
                }
                
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.toString().getBytes());
            } catch (Exception e) {
                String error = "Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
                System.err.println("[ERROR] " + error);
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("[READY] Server running on port " + port);
    }
}