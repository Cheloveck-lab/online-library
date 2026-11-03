public class Book {
    int id;
    String title, author, genre, description;
    boolean ebook;

    public Book(int id, String title, String author, String genre, String description, boolean ebook) {
        this.id = id; this.title = title; this.author = author; this.genre = genre;
        this.description = description; this.ebook = ebook;
    }
}
