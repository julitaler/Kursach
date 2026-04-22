package rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import model.LibraryOrder;
import model.LoanType;
import service.LibraryService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/orders")
public class OrderServlet extends HttpServlet {

    private final LibraryService libraryService = new LibraryService();
    private final ObjectMapper objectMapper;

    public OrderServlet() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        System.out.println("=== OrderServlet: Создание заказа");

        try {
            Long readerId = Long.parseLong(req.getParameter("readerId"));
            Long bookId = Long.parseLong(req.getParameter("bookId"));
            String typeParam = req.getParameter("type");
            LoanType loanType = LoanType.valueOf(typeParam);

            LibraryOrder order = libraryService.createOrder(readerId, bookId, loanType);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            String jsonResponse = objectMapper.writeValueAsString(order);
            resp.getWriter().write(jsonResponse);

            System.out.println("=== OrderServlet: Заказ создан ID=" + order.getId());

        } catch (Exception e) {
            System.err.println("=== OrderServlet: Ошибка: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"Ошибка: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Long readerId = Long.parseLong(req.getParameter("readerId"));

        try {
            var orders = libraryService.getReaderOrders(readerId);

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            String jsonResponse = objectMapper.writeValueAsString(orders);
            resp.getWriter().write(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"Ошибка: " + e.getMessage() + "\"}");
        }
    }
}