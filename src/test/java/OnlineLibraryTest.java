import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OnlineLibraryTest {
    private HttpServer server;
    private HttpClient client;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        LibraryRepository.books.clear();
        LibraryRepository.favorites.clear();
        SessionManager.sessions.clear();
        LibraryRepository.nextId = 8;
        LibraryRepository.books.add(new Book(1, "Тестовая книга", "Автор",
                "Фантастика", "Описание тестовой книги", true));
        LibraryRepository.books.add(new Book(2, "Бумажная книга", "Другой автор",
                "Классика", "Без электронной версии", false));

        server = OnlineLibrary.createServer(0);
        server.start();
        int port = server.getAddress().getPort();
        base = "http://127.0.0.1:" + port;
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(String path, String body, String cookie) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (cookie != null) b.header("Cookie", cookie);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String login(String username) throws Exception {
        HttpResponse<String> response = post("/?action=login",
                "login=" + username + "&password=1234", null);
        assertEquals(303, response.statusCode());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    @Test
    void htmlEscapingAndParameterParsingWork() throws Exception {
        assertEquals("&amp;&lt;&gt;&quot;&#39;", RequestUtils.esc("&<>\"'"));
        Map<String, String> values = new HashMap<>();
        RequestUtils.parseParams("name=%D0%A2%D0%B5%D1%81%D1%82&empty&x=a%2Bb", values);
        assertEquals("Тест", values.get("name"));
        assertEquals("", values.get("empty"));
        assertEquals("a+b", values.get("x"));
    }

    @Test
    void guestCatalogSearchGenreAndEmptyResultsRender() throws Exception {
        HttpResponse<String> catalog = get("/");
        assertEquals(200, catalog.statusCode());
        assertTrue(catalog.body().contains("Каталог книг"));
        assertTrue(catalog.body().contains("Тестовая книга"));
        assertTrue(catalog.body().contains("Нет электронной версии"));

        assertTrue(get("/?q=несуществующая").body().contains("Книги не найдены"));
        assertTrue(get("/?genre=Классика").body().contains("Бумажная книга"));
        assertFalse(get("/?genre=Классика").body().contains("Тестовая книга"));
        assertTrue(get("/?tab=favorites").body().contains("Войдите как читатель"));
        assertTrue(get("/?tab=login").body().contains("Вход в систему"));
    }

    @Test
    void loginFailureSuccessFavoritesAndLogoutWork() throws Exception {
        HttpResponse<String> bad = post("/?action=login", "login=reader&password=wrong", null);
        assertEquals(303, bad.statusCode());
        assertTrue(bad.headers().firstValue("Location").orElse("").contains("error=1"));

        String cookie = login("reader");
        assertTrue(getWithCookie("/?loginok=1", cookie).body().contains("Вход выполнен успешно"));

        assertEquals(303, post("/?action=fav", "id=1", cookie).statusCode());
        assertTrue(getWithCookie("/?tab=favorites", cookie).body().contains("Тестовая книга"));
        assertEquals(303, post("/?action=fav", "id=1", cookie).statusCode());
        assertTrue(getWithCookie("/?tab=favorites", cookie).body().contains("Книги не найдены"));

        HttpResponse<String> logout = getWithCookie("/?action=logout", cookie);
        assertEquals(303, logout.statusCode());
        assertTrue(get("/?logoutok=1").body().contains("Вы вышли из системы"));
    }

    private HttpResponse<String> getWithCookie(String path, String cookie) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path)).header("Cookie", cookie).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void adminCanAddDeleteAndSeeAdminPanel() throws Exception {
        String admin = login("admin");
        assertTrue(getWithCookie("/?tab=admin", admin).body().contains("Управление каталогом"));

        assertEquals(303, post("/?action=add", "title=Новая+книга&author=Автор+2&genre=Детектив&description=Описание&ebook=yes", admin).statusCode());
        assertTrue(getWithCookie("/", admin).body().contains("Новая книга"));

        assertEquals(303, post("/?action=delete", "id=8", admin).statusCode());
        assertFalse(getWithCookie("/", admin).body().contains("Новая книга"));

        // Удаление книги также очищает её идентификатор из сохранённого избранного.
        LibraryRepository.favorites.put("reader", new HashSet<>(Set.of(1)));
        assertEquals(303, post("/?action=delete", "id=1", admin).statusCode());
        assertFalse(LibraryRepository.favorites.get("reader").contains(1));
    }

    @Test
    void readingAndErrorBranchesAreHandled() throws Exception {
        assertTrue(get("/?action=read&id=1").body().contains("Демонстрационный просмотр"));
        assertTrue(get("/?action=read&id=2").body().contains("Электронная версия недоступна"));
        assertTrue(get("/?action=read&id=999").body().contains("Электронная версия недоступна"));
        assertTrue(get("/?action=read&id=bad").body().contains("Ошибка обработки запроса"));
    }

    @Test
    void administratorCannotUseReaderFavorites() throws Exception {
        String admin = login("admin");
        post("/?action=fav", "id=1", admin);
        assertFalse(LibraryRepository.favorites.containsKey("admin"));
        assertTrue(getWithCookie("/", admin).body().contains("Управление каталогом"));
    }
}
