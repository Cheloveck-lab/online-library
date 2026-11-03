import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RequestUtils {
    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    static Map<String,String> params(HttpExchange ex) throws IOException {
        Map<String,String> map = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query != null) parseParams(query, map);
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            parseParams(body, map);
        }
        return map;
    }
    static void parseParams(String data, Map<String,String> map) throws IOException {
        for (String part : data.split("&")) {
            if (part.isEmpty()) continue;
            String[] a = part.split("=", 2);
            String key = URLDecoder.decode(a[0], "UTF-8");
            String value = a.length > 1 ? URLDecoder.decode(a[1], "UTF-8") : "";
            map.put(key, value);
        }
    }
    static void redirect(HttpExchange ex) throws IOException { redirect(ex, "/"); }
    static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().add("Location", location); ex.sendResponseHeaders(303, -1); ex.close();
    }
    static void respond(HttpExchange ex, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
    }
}
