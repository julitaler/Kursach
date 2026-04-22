package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.Loan;
import service.LibraryService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/loans/*")
public class LoanServlet extends HttpServlet {

    private final LibraryService libraryService = new LibraryService();
    private final ObjectMapper objectMapper;

    public LoanServlet() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        System.out.println("=== LoanServlet: pathInfo = " + pathInfo);

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                resp.setStatus(400);
                resp.getWriter().write("{\"error\": \"Invalid path\"}");
                return;
            }

            // Разбираем путь: /{id}/{action}
            String[] parts = pathInfo.split("/");

            // parts[0] = "" (пусто из-за начального /)
            // parts[1] = id
            // parts[2] = action (issue/return)

            if (parts.length < 3) {
                resp.setStatus(400);
                resp.getWriter().write("{\"error\": \"Invalid path format\"}");
                return;
            }

            Long id = Long.parseLong(parts[1]);  // ← ИСПРАВЛЕНО: parts[1], а не parts[2]
            String action = parts[2];

            System.out.println("=== LoanServlet: id=" + id + ", action=" + action);

            if ("issue".equals(action)) {
                // Выдача книги по orderId
                Loan loan = libraryService.issueOrder(id);

                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                String jsonResponse = objectMapper.writeValueAsString(loan);
                resp.getWriter().write(jsonResponse);

                System.out.println("=== LoanServlet: Книга выдана, Loan ID=" + loan.getId());

            } else if ("return".equals(action)) {
                // Возврат книги по loanId
                Loan loan = libraryService.returnBook(id);

                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                String jsonResponse = objectMapper.writeValueAsString(loan);
                resp.getWriter().write(jsonResponse);

                System.out.println("=== LoanServlet: Книга возвращена, Loan ID=" + id);

            } else {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\": \"Unknown action: " + action + "\"}");
            }

        } catch (NumberFormatException e) {
            System.err.println("=== LoanServlet: Invalid ID format: " + e.getMessage());
            resp.setStatus(400);
            resp.getWriter().write("{\"error\": \"Invalid ID format\"}");
        } catch (Exception e) {
            System.err.println("=== LoanServlet: Ошибка: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"Ошибка: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();

        try {
            if ("/active".equals(pathInfo) || pathInfo == null) {
                // Получение активных выдач
                var loans = libraryService.getActiveLoans();

                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                String jsonResponse = objectMapper.writeValueAsString(loans);
                resp.getWriter().write(jsonResponse);

                System.out.println("=== LoanServlet: Найдено активных выдач: " + loans.size());
            } else {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\": \"Not found\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"Ошибка: " + e.getMessage() + "\"}");
        }
    }
}