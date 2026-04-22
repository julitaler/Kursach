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
    
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
    
    @Enumerated(EnumType.STRING)
    private CopyStatus status;  // AVAILABLE, LENT, RESERVED, DAMAGED
    
    private int copyNumber;
}
```

#### 3. Reader (Читатель)
```java
@Entity
public class Reader {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String fullName;
    private String email;
    private String phone;
    
    @OneToMany(mappedBy = "reader", cascade = CascadeType.ALL)
    private List<LibraryOrder> orders;
}
```

#### 4. LibraryOrder (Заказ)
```java
@Entity
@Table(name = "library_order")
public class LibraryOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "reader_id")
    private Reader reader;
    
    @ManyToOne
    @JoinColumn(name = "book_id")
    private Book book;
    
    @Enumerated(EnumType.STRING)
    private LoanType loanType;      // HOME, READING_ROOM
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;     // PENDING, ISSUED, RETURNED, CANCELLED
    
    private LocalDate createdDate;
    
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Loan loan;
}
```

#### 5. Loan (Выдача)
```java
@Entity
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private LibraryOrder order;
    
    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    private BookCopy bookCopy;
    
    private LocalDate issueDate;    // Дата выдачи
    private LocalDate dueDate;      // Плановая дата возврата
    private LocalDate returnDate;   // Фактическая дата возврата
}
```

### Схема базы данных

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│   READER     │     │  LIBRARY_ORDER   │     │     BOOK     │
├──────────────┤     ├──────────────────┤     ├──────────────┤
│ PK id        │◄────│ FK reader_id     │     │ PK id        │
│ full_name    │     │ FK book_id       │────►│ title        │
│ email        │     │ loan_type        │     │ author       │
│ phone        │     │ status           │     │ isbn         │
└──────────────┘     │ created_date     │     └──────┬───────┘
                     └────────┬─────────┘            │
                              │ 1                    │ *
                     ┌────────▼─────────┐     ┌──────▼───────┐
                     │      LOAN        │     │   BOOKCOPY   │
                     ├──────────────────┤     ├──────────────┤
                     │ PK id            │     │ PK id        │
                     │ UK order_id      │     │ FK book_id   │
                     │ FK book_copy_id  │────►│ status       │
                     │ issue_date       │     │ copy_number  │
                     │ due_date         │     └──────────────┘
                     │ return_date      │
                     └──────────────────┘
```

### Перечисления

**CopyStatus:** `AVAILABLE`, `LENT`, `RESERVED`, `DAMAGED`

**LoanType:** `HOME` (абонемент), `READING_ROOM` (читальный зал)

**OrderStatus:** `PENDING`, `ISSUED`, `RETURNED`, `CANCELLED`

---

## 🌐 REST API

### Поиск книг
```http
GET /api/books?q={query}
```
Поиск книг по названию или автору.

**Пример:**
```bash
curl "http://localhost:8080/library/api/books?q=Java"
```

---

### Создание заказа
```http
POST /api/orders?readerId={id}&bookId={id}&type={HOME|READING_ROOM}
```

**Пример:**
```bash
curl -X POST "http://localhost:8080/library/api/orders?readerId=1&bookId=2&type=HOME"
```

**Ответ:**
```json
{
  "id": 1,
  "reader": {...},
  "book": {...},
  "loanType": "HOME",
  "status": "PENDING",
  "createdDate": "2024-01-15"
}
```

---

### Получение заказов читателя
```http
GET /api/orders?readerId={id}
```

---

### Выдача книги
```http
POST /api/loans/{orderId}/issue
```

---

### Возврат книги
```http
POST /api/loans/{loanId}/return
```

---

### Получение активных выдач
```http
GET /api/loans
GET /api/loans/active
```

---

## ⚙️ Бизнес-логика

### Алгоритм выдачи книги

1. Найти заказ со статусом `PENDING`
2. Проверить наличие доступных экземпляров книги
3. Изменить статус экземпляра на `LENT`
4. Создать запись `Loan`:
   - `issueDate` = сегодня
   - `dueDate` = сегодня + 14 дней (HOME) или + 1 день (READING_ROOM)
5. Обновить статус заказа на `ISSUED`

### Алгоритм возврата книги

1. Найти активную выдачу (`Loan`)
2. Установить `returnDate` = сегодня
3. Изменить статус экземпляра на `AVAILABLE`
4. Обновить статус заказа на `RETURNED`

### Правила бизнеса

- **Абонемент (HOME):** срок выдачи 14 дней
- **Читальный зал (READING_ROOM):** срок выдачи 1 день
- Автоматическая проверка доступности экземпляров
- Транзакционная целостность всех операций

---

## 🎯 Паттерны проектирования

### 1. Singleton (ThreadLocal вариация)
**Класс:** `EntityManagerHelper`

Гарантирует один `EntityManager` на поток запроса:
```java
private static final ThreadLocal<EntityManager> threadLocal;

public static EntityManager getEntityManager() {
    EntityManager em = threadLocal.get();
    if (em == null || !em.isOpen()) {
        em = emf.createEntityManager();
        threadLocal.set(em);
    }
    return em;
}
```

### 2. Service Layer Pattern
**Класс:** `LibraryService`

Инкапсуляция бизнес-логики отдельно от контроллеров.

### 3. Repository Pattern
**Реализация:** JPA EntityManager с JPQL запросами

### 4. Listener (Observer)
**Класс:** `DataInitializerListener`

Автоматическая инициализация тестовыми данными при старте приложения.

### 5. Factory Method
**Реализация:** `Persistence.createEntityManagerFactory()`

---

## 🚀 Быстрый старт

### Требования
- Java 17+
- Maven 3.6+
- Tomcat 10.1+ (или другой Jakarta EE совместимый сервер)

### Сборка проекта

```bash
mvn clean package
```

### Запуск

1. Скопируйте `target/library.war` в директорию `webapps` Tomcat
2. Запустите Tomcat:
   ```bash
   $CATALINA_HOME/bin/startup.sh
   ```
3. Откройте браузер: `http://localhost:8080/library`

### Конфигурация БД

Приложение использует **H2 in-memory** базу данных. Данные инициализируются автоматически при старте.

**persistence.xml:**
```xml
<persistence-unit name="LibraryPU" transaction-type="RESOURCE_LOCAL">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
    
    <properties>
        <property name="jakarta.persistence.jdbc.driver" value="org.h2.Driver"/>
        <property name="jakarta.persistence.jdbc.url" 
                  value="jdbc:h2:mem:librarydb;DB_CLOSE_DELAY=-1"/>
        <property name="jakarta.persistence.jdbc.user" value="sa"/>
        <property name="jakarta.persistence.jdbc.password" value=""/>
        
        <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
        <property name="hibernate.hbm2ddl.auto" value="create-drop"/>
        <property name="hibernate.show_sql" value="true"/>
    </properties>
</persistence-unit>
```

### Тестовые данные

При старте создаются:
- **5 читателей** (ID: 1-5)
- **6 книг** с несколькими экземплярами каждой

---

## 📖 Примеры использования

### Поиск книг через UI

1. Откройте `http://localhost:8080/library/reader/search.html`
2. Введите поисковый запрос (название или автор)
3. Нажмите "Поиск"
4. Выберите книгу и оформите заказ

### Оформление заказа через API

```bash
# Создать заказ
curl -X POST "http://localhost:8080/library/api/orders?readerId=1&bookId=1&type=HOME"

# Получить заказы читателя
curl "http://localhost:8080/library/api/orders?readerId=1"

# Выдать книгу (библиотекарь)
curl -X POST "http://localhost:8080/library/api/loans/1/issue"

# Вернуть книгу (библиотекарь)
curl -X POST "http://localhost:8080/library/api/loans/1/return"
```

### Интерфейс библиотекаря

1. Авторизация: `librarian/login.html`
2. Панель управления: `librarian/dashboard.html`
3. Выдача книг: `librarian/issue.html`
4. Приём возвратов: `librarian/returns.html`

---

## 📊 Диаграмма классов

```
┌─────────────────┐       ┌─────────────────┐
│     Book        │       │     Reader      │
├─────────────────┤       ├─────────────────┤
│ - id: Long      │       │ - id: Long      │
│ - title: String │       │ - fullName: Str │
│ - author: String│       │ - email: String │
│ - isbn: String  │       │ - phone: String │
│ - copies: List  │◄──────│ - orders: List  │
└────────┬────────┘       └────────┬────────┘
         │ 1                       │ 1
         │                         │
         │ *                       │ *
┌────────▼────────┐       ┌────────▼────────┐
│   BookCopy      │       │  LibraryOrder   │
├─────────────────┤       ├─────────────────┤
│ - id: Long      │       │ - id: Long      │
│ - book: Book    │       │ - reader: Reader│
│ - status: Enum  │       │ - book: Book    │
│ - copyNumber: i │       │ - loanType: Enum│
└────────┬────────┘       │ - status: Enum  │
         │ *              │ - createdDate   │
         │                │ - loan: Loan    │
         │ 1              └────────┬────────┘
┌────────▼────────┐                │
│     Loan        │◄───────────────┘
├─────────────────┤
│ - id: Long      │
│ - order: Order  │
│ - bookCopy: Cop │
│ - issueDate     │
│ - dueDate       │
│ - returnDate    │
└─────────────────┘
```

---

## 🔧 Основные компоненты

### Контроллеры (Servlets)

| Класс | Endpoint | Методы |
|-------|----------|--------|
| `BookSearchServlet` | `/api/books` | GET (поиск) |
| `OrderServlet` | `/api/orders` | POST (создание), GET (список) |
| `LoanServlet` | `/api/loans/*` | GET, POST (выдача/возврат) |

### Сервисы

| Класс | Описание |
|-------|----------|
| `LibraryService` | Основная бизнес-логика библиотеки |
| `EntityManagerHelper` | Управление JPA EntityManager (ThreadLocal) |
| `DataInitializerListener` | Инициализация тестовых данных |

---

## 📝 Лицензия

Проект создан в образовательных целях для демонстрации работы с Jakarta EE и Hibernate.

---

## 👥 Авторы

Веб-приложение "Библиотека" - учебный проект по технологиям Jakarta EE 10, Hibernate 6.4 и H2 Database.

---

## 📞 Контакты

Для вопросов и предложений обращайтесь через Issues репозитория.
