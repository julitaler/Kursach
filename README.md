--- README.md (原始)
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

+++ README.md (修改后)
# 📚 Веб-приложение "Библиотека" - Полный технический анализ

Современное веб-приложение для управления библиотечным фондом на базе **Jakarta EE 10** и **Hibernate 6.4**.

## 📋 Оглавление

- [1. ENTITY-КЛАССЫ](#1-entity-классы)
  - [Список всех entity-классов](#список-всех-entity-классов)
  - [Детальное описание entity-классов](#детальное-описание-entity-классов)
  - [Enum-классы](#enum-классы)
- [2. СТРУКТУРА БАЗЫ ДАННЫХ](#2-структура-базы-данных)
  - [СУБД](#субд)
  - [Таблицы](#таблицы)
  - [Схема связей (ER-диаграмма)](#схема-связей-er-диаграмма)
- [3. АРХИТЕКТУРА ПРИЛОЖЕНИЯ](#3-архитектура-приложения)
  - [Структура пакетов](#структура-пакетов)
  - [REST-контроллеры (Servlets)]#rest-контроллеры-servlets)
  - [Сервисы](#сервисы)
- [4. ШАБЛОНЫ ПРОЕКТИРОВАНИЯ GoF](#4-шаблоны-проектирования-gof)
- [5. ТЕХНОЛОГИЧЕСКИЙ СТЕК](#5-технологический-стек)
- [6. БИЗНЕС-ЛОГИКА](#6-бизнес-логика)
- [7. ПРИМЕРЫ КОДА](#7-примеры-кода)
- [8. ДИАГРАММЫ](#8-диаграммы)
- [Быстрый старт](#-быстрый-старт)
- [Примеры использования](#-примеры-использования)

---

## 1. ENTITY-КЛАССЫ

### Список всех entity-классов

| Класс | Описание | Таблица |
|-------|----------|---------|
| `Book` | Книга (название, автор, ISBN) | `book` |
| `BookCopy` | Экземпляр книги | `bookcopy` |
| `Reader` | Читатель | `reader` |
| `LibraryOrder` | Заказ на книгу | `library_order` |
| `Loan` | Выдача книги | `loan` |

### Детальное описание entity-классов

#### 1.1 Book.java
```java
@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;      // Название книги
    private String author;     // Автор
    private String isbn;       // ISBN

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<BookCopy> copies = new ArrayList<>();
}
```
**Связи:** One-to-Many с `BookCopy` (одна книга → много экземпляров)

---

#### 1.2 BookCopy.java
```java
@Entity
public class BookCopy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id")
    @JsonBackReference
    private Book book;

    @Enumerated(EnumType.STRING)
    private CopyStatus status = CopyStatus.AVAILABLE;

    private int copyNumber;
}
```
**Связи:** Many-to-One с `Book`

---

#### 1.3 Reader.java
```java
@Entity
public class Reader {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;   // ФИО
    private String email;      // Email
    private String phone;      // Телефон

    @OneToMany(mappedBy = "reader", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<LibraryOrder> orders = new ArrayList<>();
}
```
**Связи:** One-to-Many с `LibraryOrder` (один читатель → много заказов)

---

#### 1.4 LibraryOrder.java
```java
@Entity
@Table(name = "library_order")
public class LibraryOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reader_id")
    @JsonBackReference
    private Reader reader;

    @ManyToOne
    @JoinColumn(name = "book_id")
    @JsonManagedReference
    private Book book;

    @Enumerated(EnumType.STRING)
    private LoanType loanType;        // HOME / READING_ROOM

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdDate = LocalDate.now();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonBackReference
    private Loan loan;
}
```
**Связи:**
- Many-to-One с `Reader`
- Many-to-One с `Book`
- One-to-One с `Loan`

---

#### 1.5 Loan.java
```java
@Entity
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private LibraryOrder order;

    @ManyToOne
    @JoinColumn(name = "book_copy_id")
    @JsonBackReference
    private BookCopy bookCopy;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;    // Дата выдачи

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;      // Дата возврата

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate returnDate;   // Фактическая дата возврата
}
```
**Связи:**
- One-to-One с `LibraryOrder`
- Many-to-One с `BookCopy`

---

### Enum-классы

#### CopyStatus.java
```java
public enum CopyStatus {
    AVAILABLE,   // Доступен
    LENT,        // Выдан
    RESERVED,    // Зарезервирован
    DAMAGED      // Поврежден
}
```

#### LoanType.java
```java
public enum LoanType {
    HOME,           // Абонемент (на дом)
    READING_ROOM    // Читальный зал
}
```

#### OrderStatus.java
```java
public enum OrderStatus {
    PENDING,     // Ожидает выдачи
    ISSUED,      // Выдан
    RETURNED,    // Возвращен
    CANCELLED    // Отменен
}
```

---

## 2. СТРУКТУРА БАЗЫ ДАННЫХ

### СУБД: H2 Database (in-memory)
**Connection URL:** `jdbc:h2:mem:librarydb;DB_CLOSE_DELAY=-1`

### Таблицы

#### 2.1 BOOK
| Поле | Тип | Описание | Ключ |
|------|-----|----------|------|
| id | BIGINT | Автоинкремент | PRIMARY KEY |
| title | VARCHAR | Название книги | |
| author | VARCHAR | Автор | |
| isbn | VARCHAR | ISBN код | |

#### 2.2 BOOKCOPY
| Поле | Тип | Описание | Ключ |
|------|-----|----------|------|
| id | BIGINT | Автоинкремент | PRIMARY KEY |
| book_id | BIGINT | Ссылка на книгу | FOREIGN KEY → BOOK(id) |
| status | VARCHAR | Статус экземпляра | |
| copy_number | INT | Номер экземпляра | |

#### 2.3 READER
| Поле | Тип | Описание | Ключ |
|------|-----|----------|------|
| id | BIGINT | Автоинкремент | PRIMARY KEY |
| full_name | VARCHAR | ФИО читателя | |
| email | VARCHAR | Email | |
| phone | VARCHAR | Телефон | |

#### 2.4 LIBRARY_ORDER
| Поле | Тип | Описание | Ключ |
|------|-----|----------|------|
| id | BIGINT | Автоинкремент | PRIMARY KEY |
| reader_id | BIGINT | Ссылка на читателя | FOREIGN KEY → READER(id) |
| book_id | BIGINT | Ссылка на книгу | FOREIGN KEY → BOOK(id) |
| loan_type | VARCHAR | Тип выдачи | |
| status | VARCHAR | Статус заказа | |
| created_date | DATE | Дата создания | |

#### 2.5 LOAN
| Поле | Тип | Описание | Ключ |
|------|-----|----------|------|
| id | BIGINT | Автоинкремент | PRIMARY KEY |
| order_id | BIGINT | Ссылка на заказ | FOREIGN KEY → LIBRARY_ORDER(id), UNIQUE |
| book_copy_id | BIGINT | Ссылка на экземпляр | FOREIGN KEY → BOOKCOPY(id) |
| issue_date | DATE | Дата выдачи | |
| due_date | DATE | Плановая дата возврата | |
| return_date | DATE | Фактическая дата возврата | |

### Схема связей (ER-диаграмма текстом)
```
READER (1) ──────< LIBRARY_ORDER >────── (1) BOOK
                       │
                       │ (1)
                       │
                       ▼ (1)
                      LOAN
                       │
                       │ (N)
                       ▼
                   BOOKCOPY ────── (N) BOOK
```

---

## 3. АРХИТЕКТУРА ПРИЛОЖЕНИЯ

### Структура пакетов
```
src/main/java/
├── model/          # Entity-классы JPA
│   ├── Book.java
│   ├── BookCopy.java
│   ├── Reader.java
│   ├── LibraryOrder.java
│   ├── Loan.java
│   ├── CopyStatus.java (enum)
│   ├── LoanType.java (enum)
│   └── OrderStatus.java (enum)
│
├── rest/           # Servlet-контроллеры (REST API)
│   ├── BookSearchServlet.java
│   ├── OrderServlet.java
│   └── LoanServlet.java
│
└── service/        # Бизнес-логика и утилиты
    ├── LibraryService.java
    ├── EntityManagerHelper.java
    └── DataInitializerListener.java

src/main/resources/
└── META-INF/
    └── persistence.xml   # JPA конфигурация

src/main/webapp/
├── WEB-INF/
│   └── web.xml
├── reader/         # UI для читателей
│   ├── search.html
│   └── orders.html
├── librarian/      # UI для библиотекарей
│   ├── dashboard.html
│   ├── issue.html
│   ├── returns.html
│   └── login.html
├── data/
│   └── info.html
├── css/
│   └── style.css
├── js/
│   └── app.js
└── index.html
```

### REST-контроллеры (Servlets)

#### 3.1 BookSearchServlet.java
**Путь:** `/api/books`

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/books?q={query}` | Поиск книг по названию или автору |

---

#### 3.2 OrderServlet.java
**Путь:** `/api/orders`

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/orders?readerId={id}&bookId={id}&type={HOME\|READING_ROOM}` | Создание заказа |
| GET | `/api/orders?readerId={id}` | Получение заказов читателя |

---

#### 3.3 LoanServlet.java
**Путь:** `/api/loans/*`

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/loans` или `/api/loans/active` | Получение активных выдач |
| POST | `/api/loans/{orderId}/issue` | Выдача книги по заказу |
| POST | `/api/loans/{loanId}/return` | Возврат книги |

---

### Сервисы

#### LibraryService.java - основные методы
| Метод | Описание |
|-------|----------|
| `List<Book> searchBooks(String query)` | Поиск книг |
| `LibraryOrder createOrder(Long readerId, Long bookId, LoanType type)` | Создание заказа |
| `Loan issueOrder(Long orderId)` | Выдача книги |
| `Loan returnBook(Long loanId)` | Возврат книги |
| `List<LibraryOrder> getReaderOrders(Long readerId)` | Заказы читателя |
| `List<Loan> getActiveLoans()` | Активные выдачи |

#### EntityManagerHelper.java - утилита управления JPA
| Метод | Описание |
|-------|----------|
| `getEntityManager()` | Получение EntityManager (ThreadLocal) |
| `closeEntityManager()` | Закрытие EntityManager |
| `beginTransaction()` | Начало транзакции |
| `commitTransaction()` | Фиксация транзакции |
| `rollbackTransaction()` | Откат транзакции |

#### DataInitializerListener.java - инициализация данных при старте
- Создает 5 тестовых читателей
- Создает 6 книг с экземплярами

---

## 4. ШАБЛОНЫ ПРОЕКТИРОВАНИЯ GoF

### 4.1 Singleton (в модификации ThreadLocal)
**Реализация:** `EntityManagerHelper.java`
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
**Описание:** Гарантирует один EntityManager на поток (request)

---

### 4.2 Factory Method
**Реализация:** Hibernate JPA Provider
```java
emf = Persistence.createEntityManagerFactory("LibraryPU");
```
**Описание:** Создание EntityManagerFactory через JPA провайдера

---

### 4.3 Listener (Observer вариация)
**Реализация:** `DataInitializerListener.java`
```java
@WebListener
public class DataInitializerListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Инициализация данных
    }
}
```
**Описание:** Реагирует на события жизненного цикла ServletContext

---

### 4.4 Service Layer Pattern
**Реализация:** `LibraryService.java`
**Описание:** Инкапсуляция бизнес-логики отдельно от контроллеров

---

### 4.5 Repository Pattern (упрощенный)
**Реализация:** JPA EntityManager через JPQL запросы
```java
em.createQuery("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.copies WHERE ...", Book.class)
```

---

## 5. ТЕХНОЛОГИЧЕСКИЙ СТЕК

| Компонент | Технология | Версия |
|-----------|------------|--------|
| **Фреймворк** | Jakarta EE (Servlet) | 6.0.0 |
| **JPA API** | Jakarta Persistence | 3.1.0 |
| **ORM** | Hibernate | 6.4.0.Final |
| **СУБД** | H2 Database | 2.2.224 (in-memory) |
| **Сборка** | Maven | - |
| **Java** | JDK | 17 |
| **JSON** | Jackson Databind | 2.15.2 |
| **Jackson JSR310** | Jackson datatype for Java 8 dates | 2.15.2 |
| **UI** | Bootstrap 5 | 5.3.0 |
| **UI Icons** | Bootstrap Icons | 1.11.0 |
| **Сервер** | Tomcat | 10.1 (совместимость) |
| **Пул соединений** | Hibernate built-in | - |

### Упаковка: WAR (`library.war`)

---

## 6. БИЗНЕС-ЛОГИКА

### 6.1 Алгоритм поиска книг
```java
// LibraryService.searchBooks()
String jpql = "SELECT DISTINCT b FROM Book b " +
              "LEFT JOIN FETCH b.copies " +
              "WHERE b.title LIKE :q OR b.author LIKE :q";
```
- Поиск по подстроке в названии ИЛИ авторе
- Eager загрузка экземпляров книги

---

### 6.2 Алгоритм создания заказа
```java
// LibraryService.createOrder()
1. Найти Reader по ID
2. Найти Book с копиями (JOIN FETCH)
3. Проверить существование
4. Создать LibraryOrder со статусом PENDING
5. Сохранить в БД
```

---

### 6.3 Алгоритм выдачи книги
```java
// LibraryService.issueOrder()
1. Найти заказ, проверить статус PENDING
2. Загрузить книгу с экземплярами
3. Найти первый AVAILABLE экземпляр
4. Изменить статус экземпляра на LENT
5. Создать Loan с датами:
   - issueDate = сегодня
   - dueDate = сегодня + 14 дней (HOME) или + 1 день (READING_ROOM)
6. Обновить статус заказа на ISSUED
```

---

### 6.4 Алгоритм возврата книги
```java
// LibraryService.returnBook()
1. Найти Loan, проверить статус ISSUED
2. Установить returnDate = сегодня
3. Изменить статус экземпляра на AVAILABLE
4. Обновить статус заказа на RETURNED
```

---

### 6.5 Работа с транзакциями
- Все операции обернуты в транзакции
- Автоматический rollback при исключениях
- ThreadLocal для хранения EntityManager

---

### 6.6 Валидация
- Проверка существования Reader/Book
- Проверка статуса заказа перед выдачей/возвратом
- Проверка доступности экземпляров
- Валидация ID читателя (1-5) на клиенте

---

## 7. ПРИМЕРЫ КОДА

### 7.1 Конфигурация подключения к БД (persistence.xml)
```xml
<persistence-unit name="LibraryPU" transaction-type="RESOURCE_LOCAL">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>

    <class>model.Book</class>
    <class>model.BookCopy</class>
    <class>model.Reader</class>
    <class>model.LibraryOrder</class>
    <class>model.Loan</class>

    <properties>
        <property name="jakarta.persistence.jdbc.driver" value="org.h2.Driver"/>
        <property name="jakarta.persistence.jdbc.url"
                  value="jdbc:h2:mem:librarydb;DB_CLOSE_DELAY=-1"/>
        <property name="jakarta.persistence.jdbc.user" value="sa"/>
        <property name="jakarta.persistence.jdbc.password" value=""/>

        <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
        <property name="hibernate.hbm2ddl.auto" value="create-drop"/>
        <property name="hibernate.show_sql" value="true"/>
        <property name="hibernate.format_sql" value="true"/>
    </properties>
</persistence-unit>
```

---

### 7.2 Контроллер с основными методами (OrderServlet.java)
```java
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

        Long readerId = Long.parseLong(req.getParameter("readerId"));
        Long bookId = Long.parseLong(req.getParameter("bookId"));
        String typeParam = req.getParameter("type");
        LoanType loanType = LoanType.valueOf(typeParam);

        LibraryOrder order = libraryService.createOrder(readerId, bookId, loanType);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String jsonResponse = objectMapper.writeValueAsString(order);
        resp.getWriter().write(jsonResponse);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Long readerId = Long.parseLong(req.getParameter("readerId"));
        var orders = libraryService.getReaderOrders(readerId);

        resp.setContentType("application/json");
        String jsonResponse = objectMapper.writeValueAsString(orders);
        resp.getWriter().write(jsonResponse);
    }
}
```

---

### 7.3 Сервис с бизнес-логикой (фрагмент LibraryService.java)
```java
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

        // Находим доступный экземпляр
        BookCopy copy = book.getCopies().stream()
                .filter(c -> c.getStatus() == CopyStatus.AVAILABLE)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No available copies"));

        copy.setStatus(CopyStatus.LENT);
        em.merge(copy);

        // Создаём выдачу
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
        return loan;
    } catch (Exception e) {
        EntityManagerHelper.rollbackTransaction();
        throw e;
    } finally {
        EntityManagerHelper.closeEntityManager();
    }
}
```

---

## 8. ДИАГРАММЫ

### 8.1 Диаграмма классов (UML текстом)
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

### 8.2 ER-диаграмма базы данных
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

## 🔧 Сводная таблица компонентов

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

### Entity-классы и связи

| Класс | Связи | Тип связи |
|-------|-------|-----------|
| `Book` | `BookCopy` | One-to-Many |
| `BookCopy` | `Book` | Many-to-One |
| `Reader` | `LibraryOrder` | One-to-Many |
| `LibraryOrder` | `Reader`, `Book`, `Loan` | Many-to-One, Many-to-One, One-to-One |
| `Loan` | `LibraryOrder`, `BookCopy` | One-to-One, Many-to-One |

---

## 📝 Лицензия

Проект создан в образовательных целях для демонстрации работы с Jakarta EE и Hibernate.

---

## 👥 Авторы

Веб-приложение "Библиотека" - учебный проект по технологиям Jakarta EE 10, Hibernate 6.4 и H2 Database.

---

## 📞 Контакты

Для вопросов и предложений обращайтесь через Issues репозитория.

---

## 📄 Содержание документа

Этот README содержит полный технический анализ проекта:

1. **ENTITY-КЛАССЫ** - Все 5 entity-классов с JPA аннотациями и связями
2. **СТРУКТУРА БАЗЫ ДАННЫХ** - 5 таблиц с полями, типами и ключами
3. **АРХИТЕКТУРА ПРИЛОЖЕНИЯ** - Структура пакетов, контроллеры, сервисы
4. **ШАБЛОНЫ ПРОЕКТИРОВАНИЯ GoF** - 5 паттернов с примерами кода
5. **ТЕХНОЛОГИЧЕСКИЙ СТЕК** - Полная таблица технологий и версий
6. **БИЗНЕС-ЛОГИКА** - Алгоритмы поиска, создания заказов, выдачи и возврата
7. **ПРИМЕРЫ КОДА** - Конфигурация БД, контроллер, сервис
8. **ДИАГРАММЫ** - UML диаграмма классов и ER-диаграмма БД

Объём документации: ~700 строк структурированной технической информации.
