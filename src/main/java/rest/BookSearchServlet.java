package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Book;
import service.LibraryService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/books")
public class BookSearchServlet extends HttpServlet {

    private final LibraryService libraryService = new LibraryService();
    private final ObjectMapper objectMapper;

    public BookSearchServlet() {
        this.objectMapper = new ObjectMapper();
        // Регистрация модуля для поддержки Java 8 дат (LocalDate)
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String query = req.getParameter("q");
        if (query == null) query = "";

        System.out.println("=== BookSearchServlet: Поиск книг: " + query);

        try {
            List<Book> books = libraryService.searchBooks(query);

            String jsonResponse = objectMapper.writeValueAsString(books);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(jsonResponse);

            System.out.println("=== BookSearchServlet: Найдено " + books.size() + " книг");

        } catch (Exception e) {
            System.err.println("=== BookSearchServlet: Ошибка: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"Ошибка сервера: " + e.getMessage() + "\"}");
        }
    }
}