import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;

public class New {
    // Hardcoded database configuration (temporary for testing)
    private static final String DB_URL = "jdbc:postgresql://dpg-d21jko7gi27c73e0jqog-a.singapore-postgres.render.com:5432/musedb_ue1o?ssl=true&sslmode=require";
    private static final String DB_USER = "musedb_ue1o_user";
    private static final String DB_PASSWORD = "pxHw8qDZSXZA2Rxi8lrxOtuzOnrPYBUq";
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        // Initialize HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Single endpoint that loads and displays all songs
        server.createContext("/", exchange -> {
            try {
                // 1. Connect to database
                StringBuilder htmlResponse = new StringBuilder();
                htmlResponse.append("<html><head><title>Music Server</title>")
                          .append("<style>body { font-family: Arial; margin: 40px; }")
                          .append("h1 { color: #333; }")
                          .append(".song { margin-bottom: 20px; padding: 15px; border-left: 4px solid #4CAF50; }")
                          .append("</style></head><body>")
                          .append("<h1>Music Library</h1>");

                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT title, singer, youtubelink FROM songs")) {

                    // 2. Build HTML response
                    while (rs.next()) {
                        htmlResponse.append("<div class='song'>")
                                  .append("<h3>").append(rs.getString("title")).append("</h3>")
                                  .append("<p><strong>Artist:</strong> ").append(rs.getString("singer")).append("</p>")
                                  .append("<a href='").append(rs.getString("youtubelink")).append("'>Listen on YouTube</a>")
                                  .append("</div>");
                    }
                }

                htmlResponse.append("</body></html>");

                // 3. Send response
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, htmlResponse.length());
                exchange.getResponseBody().write(htmlResponse.toString().getBytes());

            } catch (SQLException e) {
                String error = "<html><body><h2>Database Error</h2><p>" + e.getMessage() + "</p></body></html>";
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(500, error.length());
                exchange.getResponseBody().write(error.getBytes());
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("Server running on port " + PORT);
        System.out.println("Access the music library at: http://localhost:" + PORT + "/");
    }

    // Static block to ensure JDBC driver is loaded
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found!");
            System.exit(1);
        }
    }
}