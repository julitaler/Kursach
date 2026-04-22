package model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    @JsonBackReference  // ← Не сериализуем ссылку на заказ
    private LibraryOrder order;

    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    @JsonBackReference  // ← Не сериализуем ссылку на экземпляр книги
    private BookCopy bookCopy;

    @JsonFormat(pattern = "yyyy-MM-dd")  // ← Формат даты
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")  // ← Формат даты
    private LocalDate dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")  // ← Формат даты
    private LocalDate returnDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LibraryOrder getOrder() { return order; }
    public void setOrder(LibraryOrder order) { this.order = order; }
    public BookCopy getBookCopy() { return bookCopy; }
    public void setBookCopy(BookCopy bookCopy) { this.bookCopy = bookCopy; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
}