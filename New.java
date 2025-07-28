import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    // Hardcoded database configuration (for temporary testing)
    private static final String DB_URL = "jdbc:postgresql://dpg-d21jko7gi27c73e0jqog-a.singapore-postgres.render.com:5432/musedb_ue1o?ssl=true&sslmode=require";
    private static final String DB_USER = "musedb_ue1o_user";
    private static final String DB_PASSWORD = "pxHw8qDZSXZA2Rxi8lrxOtuzOnrPYBUq";
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        // Load PostgreSQL driver
        Class.forName("org.postgresql.Driver");
        
        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Single endpoint that shows all songs immediately
        server.createContext("/", exchange -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, singer, youtubelink FROM songs")) {
                
                // Build plain text response
                StringBuilder response = new StringBuilder("All Songs:\n\n");
                while (rs.next()) {
                    response.append("• ")
                           .append(rs.getString("title"))
                           .append(" by ")
                           .append(rs.getString("singer"))
                           .append(" | ")
                           .append(rs.getString("youtubelink"))
                           .append("\n");
                }
                
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.toString().getBytes());
                
            } catch (Exception e) {
                String error = "Database Error: " + e.getMessage();
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("Server running with HARDCODED configuration");
        System.out.println("Access songs at: http://localhost:" + PORT + "/");
    }
}