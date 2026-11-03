import com.sun.net.httpserver.HttpExchange;
import java.util.*;

public class SessionManager {
    static Map<String, String> sessions = new HashMap<>();

    static String session(HttpExchange ex) {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) for (String part : cookie.split(";")) {
            String[] a = part.trim().split("=", 2);
            if (a[0].equals("sid") && a.length > 1) return a[1];
        }
        return "";
    }

    static String user(HttpExchange ex) { return sessions.get(session(ex)); }
}
