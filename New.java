import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Properties;

public class New {
    public static void main(String[] args) throws Exception {
        // 1. Load and validate environment variables
        Properties env = validateEnvironment();

        // 2. Initialize database connection pool
        DataSource dataSource = createDataSource(
            env.getProperty("DB_URL"),
            env.getProperty("DB_USER"),
            env.getProperty("DB_PASSWORD")
        );

        // 3. Configure HTTP server
        HttpServer server = HttpServer.create(
            new InetSocketAddress(Integer.parseInt(env.getProperty("PORT", "8080"))),
            0
        );

        // 4. Register endpoints
        server.createContext("/", exchange -> handleRoot(exchange));
        server.createContext("/songs", exchange -> handleSongs(exchange, dataSource));
        server.createContext("/add", exchange -> handleAddSong(exchange, dataSource));

        server.start();
        System.out.println("Server running on port " + env.getProperty("PORT", "8080"));
    }

    private static Properties validateEnvironment() {
        Properties env = new Properties();
        env.putAll(System.getenv());

        // Debug print all variables (mask passwords)
        System.out.println("==== ENVIRONMENT VARIABLES ====");
        env.forEach((k, v) -> 
            System.out.println(k + "=" + (k.toString().contains("PASS") ? "******" : v));

        // Validate required variables
        String[] requiredVars = {"DB_URL", "DB_USER", "DB_PASSWORD"};
        for (String var : requiredVars) {
            if (env.getProperty(var) == null) {
                System.err.println("FATAL: Missing required environment variable: " + var);
                System.exit(1);
            }
        }

        // Verify SSL is enforced in DB_URL
        if (!env.getProperty("DB_URL").contains("ssl=true")) {
            System.err.println("FATAL: DB_URL must include SSL parameters");
            System.exit(1);
        }

        return env;
    }

    private static DataSource createDataSource(String url, String user, String password) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL JDBC Driver Registered!");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load JDBC driver");
            throw new SQLException(e);
        }

        org.postgresql.ds.PGSimpleDataSource ds = new org.postgresql.ds.PGSimpleDataSource();
        ds.setUrl(url);
        ds.setUser(user);
        ds.setPassword(password);
        return ds;
    }

    private static void handleRoot(HttpExchange exchange) throws IOException {
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        String response = "Music Server API\n\nEndpoints:\n"
                        + "GET /songs - List all songs\n"
                        + "POST /add?title=...&singer=...&link=... - Add new song";
        sendResponse(exchange, 200, response);
    }

    private static void handleSongs(HttpExchange exchange, DataSource dataSource) throws IOException {
        if (isHeadRequest(exchange)) return;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, title, singer, youtubelink FROM songs")) {
            
            StringBuilder response = new StringBuilder();
            while (rs.next()) {
                response.append(String.format(
                    "ID: %d | Title: %s | Artist: %s | Link: %s\n",
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("singer"),
                    rs.getString("youtubelink")
                ));
            }
            
            sendResponse(exchange, 200, response.toString());
        } catch (SQLException e) {
            handleDatabaseError(exchange, e);
        }
    }

    private static void handleAddSong(HttpExchange exchange, DataSource dataSource) throws IOException {
        if (isHeadRequest(exchange)) return;

        try {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            
            if (!params.containsKey("title") || !params.containsKey("singer") || !params.containsKey("link")) {
                sendResponse(exchange, 400, "Missing parameters. Required: title, singer, link");
                return;
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO songs(title, singer, youtubelink) VALUES(?,?,?) RETURNING id")) {
                
                stmt.setString(1, params.get("title"));
                stmt.setString(2, params.get("singer"));
                stmt.setString(3, params.get("link"));
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    sendResponse(exchange, 201, "Added song with ID: " + rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            handleDatabaseError(exchange, e);
        } catch (UnsupportedEncodingException e) {
            sendResponse(exchange, 400, "Invalid URL encoding");
        }
    }

    // Helper Methods
    private static boolean isHeadRequest(HttpExchange exchange) throws IOException {
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return true;
        }
        return false;
    }

    private static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void handleDatabaseError(HttpExchange exchange, SQLException e) throws IOException {
        System.err.println("Database Error:");
        e.printStackTrace();
        sendResponse(exchange, 500, "Database Error: " + e.getMessage());
    }

    private static Map<String, String> parseQueryParams(String query) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = pair.length > 1 ? URLDecoder.decode(pair[1], "UTF-8") : "";
                params.put(key, value);
            }
        }
        return params;
    }
}