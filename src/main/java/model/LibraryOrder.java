package model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "library_order")
public class LibraryOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reader_id")
    @JsonBackReference  // ← Не сериализуем ссылку на читателя
    private Reader reader;

    @ManyToOne
    @JoinColumn(name = "book_id")
    @JsonManagedReference  // ← Сериализуем книгу (но copies будут пропущены благодаря @JsonBackReference в BookCopy)
    private Book book;

    @Enumerated(EnumType.STRING)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @JsonFormat(pattern = "yyyy-MM-dd")  // ← Формат даты для JSON
    private LocalDate createdDate = LocalDate.now();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonBackReference  // ← Не сериализуем ссылку на Loan
    private Loan loan;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Reader getReader() { return reader; }
    public void setReader(Reader reader) { this.reader = reader; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
}