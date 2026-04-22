package service;

import model.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LibraryService {

    public List<Book> searchBooks(String query) {
        EntityManager em = null;
        try {
            em = EntityManagerHelper.getEntityManager();
            System.out.println("=== LibraryService: Поиск книг: " + query);

            String jpql = "SELECT DISTINCT b FROM Book b " +
                    "LEFT JOIN FETCH b.copies " +
                    "WHERE b.title LIKE :q OR b.author LIKE :q";

            List<Book> books = em.createQuery(jpql, Book.class)
                    .setParameter("q", "%" + query + "%")
                    .getResultList();

            System.out.println("=== LibraryService: Найдено книг: " + books.size());
            return books;
        } catch (Exception e) {
            System.err.println("=== LibraryService: Ошибка при поиске: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (em != null) {
                EntityManagerHelper.closeEntityManager();
            }
        }
    }

    public LibraryOrder createOrder(Long readerId, Long bookId, LoanType type) {
        EntityManager em = EntityManagerHelper.getEntityManager();
        try {
            EntityManagerHelper.beginTransaction();

            Reader reader = em.find(Reader.class, readerId);

            // 🔧 ИСПРАВЛЕНО: загружаем книгу с copies через JOIN FETCH
            Book book = em.createQuery(
                            "SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.copies WHERE b.id = :bookId",
                            Book.class)
                    .setParameter("bookId", bookId)
                    .getSingleResult();

            if (reader == null || book == null) {
                throw new IllegalArgumentException("Reader or Book not found");
            }

            LibraryOrder order = new LibraryOrder();
            order.setReader(reader);
            order.setBook(book);
            order.setLoanType(type);
            em.persist(order);

            EntityManagerHelper.commitTransaction();
            System.out.println("=== LibraryService: Заказ создан ID=" + order.getId());
            return order;
        } catch (Exception e) {
            EntityManagerHelper.rollbackTransaction();
            System.err.println("=== LibraryService: Ошибка при создании заказа: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            EntityManagerHelper.closeEntityManager();
        }
    }

    public Loan issueOrder(Long orderId) {
        EntityManager em = EntityManagerHelper.getEntityManager();
        try {
            EntityManagerHelper.beginTransaction();

            LibraryOrder order = em.find(LibraryOrder.class, orderId);

            if (order == null || order.getStatus() != OrderStatus.PENDING) {
                throw new IllegalStateException("Order not found or already processed");
            }

            // Загружаем книгу с копиями
            Book book = em.createQuery(
                            "SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.copies WHERE b.id = :bookId",
                            Book.class)
                    .setParameter("bookId", order.getBook().getId())
                    .getSingleResult();

            BookCopy copy = book.getCopies().stream()
                    .filter(c -> c.getStatus() == CopyStatus.AVAILABLE)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No available copies"));

            copy.setStatus(CopyStatus.LENT);
            em.merge(copy);

            Loan loan = new Loan();
            loan.setOrder(order);
            loan.setBookCopy(copy);
            loan.setIssueDate(LocalDate.now());
            loan.setDueDate(order.getLoanType() == LoanType.HOME ?
                    LocalDate.now().plusDays(14) : LocalDate.now().plusDays(1));
            em.persist(loan);

            order.setLoan(loan);
            order.setStatus(OrderStatus.ISSUED);
            em.merge(order);

            EntityManagerHelper.commitTransaction();
            System.out.println("=== LibraryService: Книга выдана, Loan ID=" + loan.getId());
            return loan;
        } catch (Exception e) {
            EntityManagerHelper.rollbackTransaction();
            System.err.println("=== LibraryService: Ошибка при выдаче: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            EntityManagerHelper.closeEntityManager();
        }
    }

    public Loan returnBook(Long loanId) {
        EntityManager em = EntityManagerHelper.getEntityManager();
        try {
            EntityManagerHelper.beginTransaction();

            Loan loan = em.find(Loan.class, loanId);

            if (loan == null || loan.getOrder().getStatus() != OrderStatus.ISSUED) {
                throw new IllegalStateException("Invalid loan for return");
            }

            loan.setReturnDate(LocalDate.now());
            loan.getBookCopy().setStatus(CopyStatus.AVAILABLE);
            loan.getOrder().setStatus(OrderStatus.RETURNED);

            em.merge(loan);

            EntityManagerHelper.commitTransaction();
            System.out.println("=== LibraryService: Книга возвращена, Loan ID=" + loanId);
            return loan;
        } catch (Exception e) {
            EntityManagerHelper.rollbackTransaction();
            System.err.println("=== LibraryService: Ошибка при возврате: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            EntityManagerHelper.closeEntityManager();
        }
    }

    public List<LibraryOrder> getReaderOrders(Long readerId) {
        EntityManager em = EntityManagerHelper.getEntityManager();
        try {
            System.out.println("=== LibraryService: Получение заказов читателя: " + readerId);

            List<LibraryOrder> orders = em.createQuery(
                            "SELECT DISTINCT o FROM LibraryOrder o " +
                                    "LEFT JOIN FETCH o.book b " +
                                    "LEFT JOIN FETCH b.copies " +
                                    "LEFT JOIN FETCH o.loan " +
                                    "WHERE o.reader.id = :readerId",
                            LibraryOrder.class)
                    .setParameter("readerId", readerId)
                    .getResultList();

            System.out.println("=== LibraryService: Найдено заказов: " + orders.size());
            return orders;
        } catch (Exception e) {
            System.err.println("=== LibraryService: Ошибка при получении заказов: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            EntityManagerHelper.closeEntityManager();
        }
    }

    public List<Loan> getActiveLoans() {
        EntityManager em = EntityManagerHelper.getEntityManager();
        try {
            System.out.println("=== LibraryService: Получение активных выдач");

            List<Loan> loans = em.createQuery(
                            "SELECT DISTINCT l FROM Loan l " +
                                    "LEFT JOIN FETCH l.order o " +
                                    "LEFT JOIN FETCH o.reader " +
                                    "LEFT JOIN FETCH o.book " +
                                    "WHERE o.status = 'ISSUED'",
                            Loan.class)
                    .getResultList();

            System.out.println("=== LibraryService: Активных выдач: " + loans.size());
            return loans;
        } catch (Exception e) {
            System.err.println("=== LibraryService: Ошибка при получении выдач: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            EntityManagerHelper.closeEntityManager();
        }
    }
}