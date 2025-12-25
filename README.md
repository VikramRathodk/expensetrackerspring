# Expense Tracker Backend

A backend application for managing personal expenses, built using **Spring Boot** and **Kotlin**. This project provides RESTful APIs to create, update, delete, and retrieve expenses, supports bulk inserts, validation, global exception handling, and integrates with **PostgreSQL**.

---

## 🚀 Features

* CRUD operations for expenses
* Bulk insert of expenses
* Input validation using Jakarta Bean Validation
* Global exception handling with consistent API responses
* PostgreSQL integration using Spring Data JPA
* Clean layered architecture (Controller, Service, Repository)
* Ready for enhancements like pagination, filtering, and WebSocket updates

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **Framework:** Spring Boot
* **Database:** PostgreSQL
* **ORM:** Spring Data JPA (Hibernate)
* **Build Tool:** Gradle (Kotlin DSL)
* **Validation:** spring-boot-starter-validation

---

## 📁 Project Structure

```
com.devvikram.expensetracker
├── controllers      # REST controllers
├── service          # Business logic
├── repository       # JPA repositories
├── models           # Entities and API response models
├── exceptions       # Custom exceptions & global handler
└── ExpensetrackerApplication.kt
```

---

## 🔗 API Endpoints

### ➕ Create Expense

```
POST /api/expenses
```

### ➕ Bulk Insert Expenses

```
POST /api/expenses/bulk
```

### 📄 Get All Expenses

```
GET /api/expenses
```

### ✏️ Update Expense

```
PUT /api/expenses/{id}
```

### ❌ Delete Expense

```
DELETE /api/expenses/{id}
```

---

## 📦 Sample Request (Create Expense)

```json
{
  "title": "Coffee",
  "amount": 120,
  "category": "Food",
  "note": "Morning coffee"
}
```

---

## ✅ Sample Success Response

```json
{
  "status": true,
  "message": "Expense added successfully",
  "data": {
    "id": 1,
    "title": "Coffee",
    "amount": 120.0,
    "category": "Food",
    "note": "Morning coffee",
    "createdAt": "2025-12-19T06:30:00"
  }
}
```

---

## ❌ Validation Error Response

```json
{
  "status": false,
  "message": "Validation failed",
  "data": {
    "amount": "Amount must be greater than 0",
    "title": "Title is required"
  }
}
```

---

## ⚙️ Configuration

### `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/expensetracker
spring.datasource.username=postgres
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## ▶️ How to Run

1. Make sure **PostgreSQL** is running
2. Create database: `expensetracker`
3. Update DB credentials in `application.properties`
4. Run the application:

```
./gradlew bootRun
```

App will start at:

```
http://localhost:8080
```

---

## 🧠 Future Enhancements

* Pagination & sorting
* Filter expenses by date/category
* WebSocket-based real-time updates
* JWT-based authentication & authorization
* Docker support

---

## 👤 Author

**Vikram Rathod**
Android & Backend Developer

---

## 📄 License

This project is for learning and portfolio purposes.
