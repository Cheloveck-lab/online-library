import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;

public class LibraryHandler implements HttpHandler {
    private final HtmlRenderer renderer = new HtmlRenderer();

    @Override public void handle(HttpExchange ex) throws IOException {
        try {
            Map<String,String> p = RequestUtils.params(ex);
            String action = p.getOrDefault("action", "");
            if ("login".equals(action)) {
                String login=p.getOrDefault("login", ""), pass=p.getOrDefault("password", "");
                if ((login.equals("reader") || login.equals("admin")) && pass.equals("1234")) {
                    String sid=UUID.randomUUID().toString(); SessionManager.sessions.put(sid, login);
                    ex.getResponseHeaders().add("Set-Cookie", "sid="+sid+"; Path=/; HttpOnly; SameSite=Lax");
                    RequestUtils.redirect(ex, "/?loginok=1");
                } else RequestUtils.redirect(ex, "/?tab=login&error=1");
                return;
            }
            if ("logout".equals(action)) {
                SessionManager.sessions.remove(SessionManager.session(ex));
                ex.getResponseHeaders().add("Set-Cookie", "sid=; Path=/; Max-Age=0");
                RequestUtils.redirect(ex, "/?logoutok=1"); return;
            }
            String who=SessionManager.user(ex);
            if ("fav".equals(action) && who != null && !who.equals("admin")) {
                int id=Integer.parseInt(p.getOrDefault("id", "0"));
                Set<Integer> set=LibraryRepository.favorites.computeIfAbsent(who,k->new HashSet<>());
                if (!set.add(id)) set.remove(id);
                RequestUtils.redirect(ex); return;
            }
            if ("add".equals(action) && "admin".equals(who)) {
                LibraryRepository.books.add(new Book(LibraryRepository.nextId++, p.getOrDefault("title","Новая книга"), p.getOrDefault("author","Неизвестный автор"), p.getOrDefault("genre","Другое"), p.getOrDefault("description","Описание отсутствует"), "yes".equals(p.get("ebook"))));
                RequestUtils.redirect(ex); return;
            }
            if ("delete".equals(action) && "admin".equals(who)) {
                int id=Integer.parseInt(p.getOrDefault("id", "0"));
                LibraryRepository.books.removeIf(b->b.id==id);
                LibraryRepository.favorites.values().forEach(set->set.remove(id));
                RequestUtils.redirect(ex); return;
            }
            if ("read".equals(action)) {
                int id=Integer.parseInt(p.getOrDefault("id", "0"));
                Book book=LibraryRepository.books.stream().filter(b->b.id==id).findFirst().orElse(null);
                String msg=(book!=null && book.ebook) ? "<h1>Демонстрационный просмотр</h1><h2>"+RequestUtils.esc(book.title)+"</h2><p>"+RequestUtils.esc(book.description)+"</p><p>Полный текст электронной книги в учебный прототип не включён.</p>" : "<h1>Электронная версия недоступна</h1>";
                RequestUtils.respond(ex, "<!doctype html><html lang='ru'><meta charset='utf-8'><body style='font:18px Arial;max-width:800px;margin:60px auto;line-height:1.6'>"+msg+"<p><a href='/'>Вернуться в каталог</a></p></body></html>"); return;
            }
            RequestUtils.respond(ex, renderer.page(ex,p));
        } catch (Exception e) {
            RequestUtils.respond(ex, "<!doctype html><html lang='ru'><meta charset='utf-8'><h1>Ошибка обработки запроса</h1><p>Проверьте введённые данные.</p><a href='/'>На главную</a></html>");
        }
    }
}
