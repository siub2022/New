import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;

public class Music  {
    // ==================== SAFETY NETS ====================
    // Layer 1: Hardcoded DEFAULTS (for local testing ONLY)
    private static final int PORT = 8080;
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost/test_db";
    private static final String DEFAULT_DB_USER = "test_user";
    private static final String DEFAULT_DB_PASS = "test_pass";

    // Layer 2: Environment Variables (Production)
    private static String getDbUrl() {
        String url = System.getenv("DB_URL");
        return url != null ? url : DEFAULT_DB_URL;
    }

    private static String getDbUser() {
        String user = System.getenv("DB_USER");
        return user != null ? user : DEFAULT_DB_USER;
    }

    private static String getDbPassword() {
        String pass = System.getenv("DB_PASSWORD");
        return pass != null ? pass : DEFAULT_DB_PASS;
    }

    // Layer 3: Runtime Warnings
    private static void checkSecurity() {
        if (getDbUrl().equals(DEFAULT_DB_URL)) {
            System.err.println("⚠️ 警告: 使用备用数据库! 请检查Render环境变量!");
        }
    }
    // =====================================================

    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("[数据库] 驱动程序加载成功");
        } catch (ClassNotFoundException e) {
            System.err.println("[错误] 找不到PostgreSQL驱动!");
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        checkSecurity(); // Security check
        
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        server.createContext("/", exchange -> {
            try {
                String singerParam = exchange.getRequestURI().getQuery();
                String sql = singerParam != null && singerParam.startsWith("singer=") 
                    ? "SELECT singer, title, youtubelink FROM songs WHERE singer LIKE ? ORDER BY title"
                    : "SELECT singer, title, youtubelink FROM songs ORDER BY singer, title";

                try (Connection conn = DriverManager.getConnection(
                        getDbUrl(), getDbUser(), getDbPassword());
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    if (singerParam != null && singerParam.startsWith("singer=")) {
                        stmt.setString(1, "%" + URLDecoder.decode(singerParam.substring(7), "UTF-8") + "%");
                    }

                    StringBuilder html = new StringBuilder("""
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <title>音乐数据库</title>
                            <style>
                                body { font-family: 'Microsoft YaHei', sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
                                .song { background: #fff; padding: 15px; margin-bottom: 10px; border-radius: 5px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                                h1 { color: #e74c3c; }
                                .youtube { color: #ff0000; text-decoration: none; font-weight: bold; }
                            </style>
                        </head>
                        <body>
                            <h1>🎵 我的音乐库</h1>
                            <form action="/">
                                <input type="text" name="singer" placeholder="搜索歌手...">
                                <button type="submit">搜索</button>
                                <a href="/">显示全部</a>
                            </form>
                        """);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            html.append(String.format(
                                "<div class='song'><b>%s</b><br>%s<br><a class='youtube' href='%s' target='_blank'>▶ YouTube观看</a></div>",
                                rs.getString("singer"),
                                rs.getString("title"),
                                rs.getString("youtubelink")
                            ));
                        }
                    }

                    html.append("</body></html>");
                    sendResponse(exchange, 200, html.toString());
                }
            } catch (Exception e) {
                sendError(exchange, 500, "服务器错误: " + e.getMessage());
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.printf("[系统] 服务已启动: http://localhost:%d%n", PORT);
    }

    private static void sendResponse(HttpExchange exchange, int code, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String errorPage = "<html><body><h1>错误</h1><p>" + message + "</p></body></html>";
        sendResponse(exchange, code, errorPage);
    }
}