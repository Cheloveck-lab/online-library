import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class OnlineLibrary {
    static HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new LibraryHandler());
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r); thread.setDaemon(true); return thread;
        }));
        return server;
    }
    public static void main(String[] args) throws Exception {
        int port=args.length>0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server=createServer(port); server.start();
        System.out.println("Онлайн-библиотека запущена: http://localhost:"+port);
    }
}
