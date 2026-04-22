package service;

import model.*;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.persistence.EntityManager;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class DataInitializerListener implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(DataInitializerListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("=== Инициализация данных библиотеки ===");

        EntityManager em = EntityManagerHelper.getEntityManager();
        try {
            EntityManagerHelper.beginTransaction();

            // Проверяем, есть ли уже данные
            Long count = em.createQuery("SELECT COUNT(b) FROM Book b", Long.class)
                    .getSingleResult();

            if (count > 0) {
                logger.info("Данные уже существуют");
                EntityManagerHelper.commitTransaction();
                return;
            }

            // Создаем читателей
            createReader(em, "Иванов Иван Иванович", "ivanov@mail.ru", "+7-900-111-22-33");
            createReader(em, "Петрова Мария Сергеевна", "petrova@mail.ru", "+7-900-222-33-44");
            createReader(em, "Сидоров Алексей Петрович", "sidorov@mail.ru", "+7-900-333-44-55");
            createReader(em, "Козлова Елена Дмитриевна", "kozlova@mail.ru", "+7-900-444-55-66");
            createReader(em, "Новиков Дмитрий Александрович", "novikov@mail.ru", "+7-900-555-66-77");

            // Создаем книги
            Book book1 = createBook(em, "Евгений Онегин", "Александр Сергеевич Пушкин", "978-5-699-12345-1");
            addBookCopies(em, book1, 3);

            Book book2 = createBook(em, "Капитанская дочка", "Александр Сергеевич Пушкин", "978-5-699-12346-8");
            addBookCopies(em, book2, 2);

            Book book3 = createBook(em, "Герой нашего времени", "Михаил Юрьевич Лермонтов", "978-5-699-12347-5");
            addBookCopies(em, book3, 2);

            Book book4 = createBook(em, "Война и мир", "Лев Николаевич Толстой", "978-5-699-12348-2");
            addBookCopies(em, book4, 4);

            Book book5 = createBook(em, "Анна Каренина", "Лев Николаевич Толстой", "978-5-699-12349-9");
            addBookCopies(em, book5, 2);

            Book book6 = createBook(em, "Преступление и наказание", "Фёдор Михайлович Достоевский", "978-5-699-12350-5");
            addBookCopies(em, book6, 3);

            EntityManagerHelper.commitTransaction();
            logger.info("=== Инициализация завершена успешно ===");

        } catch (Exception e) {
            EntityManagerHelper.rollbackTransaction();
            logger.log(Level.SEVERE, "Ошибка при инициализации", e);
        } finally {
            EntityManagerHelper.closeEntityManager();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Приложение остановлено");
    }

    private void createReader(EntityManager em, String fullName, String email, String phone) {
        Reader reader = new Reader();
        reader.setFullName(fullName);
        reader.setEmail(email);
        reader.setPhone(phone);
        em.persist(reader);
        logger.info("Создан читатель: " + fullName);
    }

    private Book createBook(EntityManager em, String title, String author, String isbn) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        em.persist(book);
        logger.info("Создана книга: " + title);
        return book;
    }

    private void addBookCopies(EntityManager em, Book book, int count) {
        for (int i = 1; i <= count; i++) {
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            copy.setCopyNumber(i);
            copy.setStatus(CopyStatus.AVAILABLE);
            em.persist(copy);
        }
        logger.info("  Добавлено " + count + " экземпляров для: " + book.getTitle());
    }
}