import com.sun.net.httpserver.HttpExchange;
import java.util.*;

public class HtmlRenderer {
    String page(HttpExchange ex, Map<String, String> p) {
        String who = SessionManager.user(ex);
        String search = p.getOrDefault("q", "").trim();
        String genre = p.getOrDefault("genre", "Все");

        boolean admin = "admin".equals(who);
        String tab = p.getOrDefault("tab", "catalog");

        StringBuilder h = new StringBuilder("""
<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Онлайн-библиотека</title>

<style>
:root {
    --ink: #172b24;
    --muted: #69766e;
    --green: #22664d;
    --bg: #f5f7f2;
    --line: #e2e8df;
}

* {
    box-sizing: border-box;
}

body {
    margin: 0;
    background: var(--bg);
    color: var(--ink);
    font: 16px Arial, sans-serif;
}

.wrap {
    max-width: 1120px;
    margin: auto;
    padding: 24px;
}

.top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    flex-wrap: wrap;
}

.brand {
    font-size: 25px;
    font-weight: 800;
}

.brand span {
    color: var(--green);
}

.muted {
    color: var(--muted);
}

.hero {
    margin: 24px 0;
    padding: 30px;
    border-radius: 22px;
    background: #e5eee4;
}

.hero h1 {
    font-size: 34px;
    margin: 0 0 8px;
}

.nav {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin: 20px 0;
}

.nav a, .btn {
    border: 0;
    border-radius: 10px;
    padding: 10px 15px;
    background: var(--green);
    color: white;
    text-decoration: none;
    cursor: pointer;
    font-weight: 700;
}

.nav a.alt, .btn.alt {
    background: white;
    color: var(--ink);
    border: 1px solid var(--line);
}

.tools {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    margin: 20px 0;
}

.tools input, .tools select, input, textarea {
    padding: 11px;
    border: 1px solid #ccd7ce;
    border-radius: 9px;
    font: inherit;
}

.tools input {
    flex: 1;
    min-width: 180px;
}

.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 15px;
}

.book, .panel {
    background: white;
    border: 1px solid var(--line);
    border-radius: 16px;
    padding: 18px;
}

.cover {
    height: 110px;
    border-radius: 12px;
    background: linear-gradient(135deg, #dbe9d9, #f2e7d0);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 38px;
    margin-bottom: 15px;
}

.book h3 {
    margin: 0 0 6px;
}

.tag {
    display: inline-block;
    font-size: 12px;
    background: #edf4ed;
    padding: 5px 8px;
    border-radius: 20px;
    margin: 4px 0 10px;
}

.row {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
}

.small {
    font-size: 13px;
}

.panel {
    margin: 18px 0;
}

.panel form {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin: 10px 0;
}

.panel input {
    flex: 1;
    min-width: 140px;
}

.footer {
    padding: 25px 0;
    color: var(--muted);
    font-size: 13px;
}

@media (max-width: 600px) {
    .wrap {
        padding: 15px;
    }

    .hero h1 {
        font-size: 27px;
    }
}
</style>
</head>

<body>
<main class="wrap">
""");

        h.append("<div class='top'><div class='brand'><span>Читай</span>Город</div><div class='muted'>");

        h.append(who == null
                ? "Гостевой доступ"
                : "Вы вошли как <b>" + RequestUtils.esc(who) + "</b>");

        h.append("</div></div>");

        h.append("""
<section class="hero">
    <h1>Книги, которые открывают мир</h1>
    <div>Ищите любимые произведения, изучайте каталог и сохраняйте книги в избранное.</div>
    <div class="muted small" style="margin-top:14px">
        Демонстрационный прототип · данные хранятся в памяти до остановки приложения
    </div>
</section>
""");

        h.append("<div class='nav'>");
        h.append("<a href='/'>Каталог</a>");
        h.append("<a class='alt' href='/?tab=favorites'>Избранное</a>");

        if (admin) {
            h.append("<a class='alt' href='/?tab=admin'>Управление каталогом</a>");
        }

        if (who == null) {
            h.append("<a class='alt' href='/?tab=login'>Вход</a>");
        } else {
            h.append("<a class='alt' href='/?action=logout'>Выйти</a>");
        }

        h.append("</div>");

        if ("1".equals(p.get("loginok")) && who != null) {
            h.append("""
<div class="panel" style="border-color:#7ab58c;background:#f0faf2">
    <b>Вход выполнен успешно.</b>
    Теперь можно добавлять книги в избранное.
</div>
""");
        }

        if ("1".equals(p.get("logoutok")) && who == null) {
            h.append("<div class='panel'>Вы вышли из системы.</div>");
        }

        // Страница входа
        if ("login".equals(tab) && who == null) {

            if ("1".equals(p.get("error"))) {
                h.append("""
<div class="panel" style="border-color:#d88;background:#fff5f5">
    <b>Не удалось войти.</b>
    Проверьте логин и пароль.
</div>
""");
            }

            h.append("""
<section class="panel">
    <h2>Вход в систему</h2>
    <p class="muted">
        Демо-аккаунты: читатель <b>reader</b> или администратор <b>admin</b>.
        Пароль для обоих: <b>1234</b>.
    </p>

    <form method="post" action="/?action=login">
        <input name="login" placeholder="Логин" required>
        <input name="password" type="password" placeholder="Пароль" required>
        <button class="btn">Войти</button>
    </form>
</section>
""");

            // Панель администратора
        } else if ("admin".equals(tab) && admin) {

            h.append("""
<section class="panel">
    <h2>Управление каталогом</h2>
    <p class="muted">
        Добавление книги доступно в деморежиме администратора.
    </p>

    <form method="post" action="/?action=add">
        <input name="title" placeholder="Название книги" required>
        <input name="author" placeholder="Автор" required>
        <input name="genre" placeholder="Жанр" required>
        <input name="description" placeholder="Описание" required>

        <label>
            <input type="checkbox" name="ebook" value="yes">
            Электронная версия
        </label>

        <button class="btn">Добавить книгу</button>
    </form>

    <h3>Книги в каталоге</h3>
""");

            for (Book b : LibraryRepository.books) {
                h.append(
                        "<div class='row' style='padding:8px 0;border-bottom:1px solid var(--line)'>"
                                + "<span style='flex:1'>"
                                + RequestUtils.esc(b.title) + " — " + RequestUtils.esc(b.author)
                                + "</span>"
                                + "<form method='post' action='/?action=delete' style='margin:0'>"
                                + "<input type='hidden' name='id' value='" + b.id + "'>"
                                + "<button class='btn alt'>Удалить</button>"
                                + "</form></div>"
                );
            }

            h.append("</section>");

        } else {

            Set<Integer> fav = who == null
                    ? Collections.emptySet()
                    : LibraryRepository.favorites.getOrDefault(who, new HashSet<>());

            // Избранное
            if ("favorites".equals(tab)) {
                h.append("<h2>Избранное</h2>");

                if (who == null) {
                    h.append("""
<p>
    Войдите как читатель, чтобы сохранять избранное.
    <a href="/?tab=login">Войти</a>
</p>
""");
                }

            } else {
                // Каталог
                h.append("<h2>Каталог книг</h2>");

                if (who == null) {
                    h.append("""
<p class="muted">
    Чтобы добавить книгу в избранное, откройте «Вход»
    и войдите как <b>reader</b> с паролем <b>1234</b>.
</p>
""");
                }

                h.append("<form class='tools' method='get'>");
                h.append("<input name='q' value='" + RequestUtils.esc(search)
                        + "' placeholder='Поиск по названию, автору или описанию'>");

                h.append("<select name='genre'><option>Все</option>");

                Set<String> gs = new TreeSet<>();

                for (Book b : LibraryRepository.books) {
                    gs.add(b.genre);
                }

                for (String g : gs) {
                    h.append("<option "
                            + (g.equals(genre) ? "selected" : "")
                            + ">"
                            + RequestUtils.esc(g)
                            + "</option>");
                }

                h.append("</select><button class='btn'>Найти</button></form>");
            }

            h.append("<div class='grid'>");

            int count = 0;

            for (Book b : LibraryRepository.books) {

                if ("favorites".equals(tab) && !fav.contains(b.id)) {
                    continue;
                }

                if (!"favorites".equals(tab)
                        && !(b.title + " " + b.author + " " + b.description)
                        .toLowerCase(Locale.ROOT)
                        .contains(search.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                if (!"favorites".equals(tab)
                        && !genre.equals("Все")
                        && !genre.equals(b.genre)) {
                    continue;
                }

                count++;

                h.append("<article class='book'>");
                h.append("<div class='cover'>Книга</div>");
                h.append("<span class='tag'>" + RequestUtils.esc(b.genre) + "</span>");
                h.append("<h3>" + RequestUtils.esc(b.title) + "</h3>");
                h.append("<div class='muted'>" + RequestUtils.esc(b.author) + "</div>");
                h.append("<p>" + RequestUtils.esc(b.description) + "</p>");
                h.append("<div class='row'>");

                if (b.ebook) {
                    h.append("<a class='btn' href='/?action=read&id="
                            + b.id + "'>Читать</a>");
                } else {
                    h.append("<span class='tag'>Нет электронной версии</span>");
                }

                // Кнопка избранного видна вошедшему читателю
                if (who != null && !admin) {
                    h.append(
                            "<form method='post' action='/?action=fav' style='margin:0'>"
                                    + "<input type='hidden' name='id' value='" + b.id + "'>"
                                    + "<button class='btn alt'>"
                                    + (fav.contains(b.id)
                                    ? "Убрать из избранного"
                                    : "♡ В избранное")
                                    + "</button></form>"
                    );
                }

                h.append("</div></article>");
            }

            h.append("</div>");

            if (count == 0) {
                h.append("""
<p class="muted">
    Книги не найдены. Измените поиск или выберите другой раздел.
</p>
""");
            }
        }

        h.append("""
<div class="footer">
    Учебный прототип онлайн-библиотеки · Java HTTP Server · Каталог демонстрационный
</div>
</main>
</body>
</html>
""");

        return h.toString();
    }
}
