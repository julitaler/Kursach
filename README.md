# 📚 Веб-приложение "Библиотека"
Современное веб-приложение для управления библиотечным фондом на базе **Jakarta EE 10** и **Hibernate 6.4**.
## 📋 Оглавление
- [Технологический стек](#-технологический-стек)
- [Архитектура приложения](#-архитектура-приложения)
- [Модель данных](#-модель-данных)
- [REST API](#-rest-api)
- [Бизнес-логика](#-бизнес-логика)
- [Паттерны проектирования](#-паттерны-проектирования)
- [Быстрый старт](#-быстрый-старт)
- [Примеры использования](#-примеры-использования)
---
## 🛠 Технологический стек
| Компонент | Технология | Версия |
|-----------|------------|--------|
| **Фреймворк** | Jakarta EE (Servlet) | 6.0.0 |
| **JPA API** | Jakarta Persistence | 3.1.0 |
| **ORM** | Hibernate | 6.4.0.Final |
| **СУБД** | H2 Database | 2.2.224 (in-memory) |
| **Сборка** | Maven | - |
| **Java** | JDK | 17 |
| **JSON** | Jackson Databind | 2.15.2 |
| **UI** | Bootstrap 5 | 5.3.0 |
| **Сервер** | Tomcat | 10.1 |
---
## 🏗 Архитектура приложения
```
src/main/java/
├── model/          # Entity-классы JPA
│   ├── Book.java
│   ├── BookCopy.java
│   ├── Reader.java
│   ├── LibraryOrder.java
│   ├── Loan.java
│   └── enums/      # CopyStatus, LoanType, OrderStatus
│
├── rest/           # REST контроллеры (Servlets)
│   ├── BookSearchServlet.java
│   ├── OrderServlet.java
│   └── LoanServlet.java
│
└── service/        # Бизнес-логика
    ├── LibraryService.java
    ├── EntityManagerHelper.java
    └── DataInitializerListener.java
src/main/webapp/
├── reader/         # UI для читателей
├── librarian/      # UI для библиотекарей
├── css/            # Стили
├── js/             # JavaScript
└── index.html      # Главная страница
```
### Слои архитектуры
1. **Presentation Layer** - HTML/Bootstrap UI + JavaScript
2. **Controller Layer** - Servlets (`/api/*`)
3. **Service Layer** - `LibraryService` (бизнес-логика)
4. **Repository Layer** - JPA EntityManager
5. **Entity Layer** - JPA модели данных
---
## 💾 Модель данных
### Entity-классы
#### 1. Book (Книга)
```java
@Entity
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;      // Название
    private String author;     // Автор
    private String isbn;       // ISBN
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<BookCopy> copies;
}
```
#### 2. BookCopy (Экземпляр книги)
```java
@Entity
public class BookCopy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
