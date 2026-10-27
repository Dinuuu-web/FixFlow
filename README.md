# 🔧 FixFlow — Smart Campus Issue Resolution Platform

A full-stack Java web application for managing campus maintenance complaints, built with **Spring Boot 3**, **MySQL**, **Thymeleaf**, and **Google Gemini AI**.

---

## 📋 Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Demo Credentials](#demo-credentials)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)
- [Architecture](#architecture)
- [Known Limitations](#known-limitations)

---

## ✅ Features

### Students
- Register, log in, and manage their own complaints
- Submit campus maintenance complaints with image attachments
- AI-powered complaint improvement, category/priority suggestions, and troubleshooting
- Duplicate complaint detection before submission
- Cancel pending complaints
- Track status history with full audit timeline

### Administrators
- Full complaint management dashboard with real-time analytics
- Filter by status, priority, and category
- Assign complaints to maintenance staff
- Update complaint statuses with remarks
- Monitor overdue complaints (SLA-based)
- View category and status distribution charts
- Manage departments
- AI-generated complaint summaries

### Maintenance Staff
- View all assigned complaints
- Update status to IN_PROGRESS or RESOLVED
- Add work notes and resolution remarks

### AI Assistant (Gemini)
- **Improve Complaint**: Rewrites drafts professionally
- **Suggest Category**: Recommends the best category
- **Suggest Priority**: Recommends LOW/MEDIUM/HIGH/CRITICAL
- **Troubleshooting Guidance**: Safe, non-dangerous steps
- **Admin Summary**: Brief summary + next action (admin only)
- Gracefully degrades when API key is not set

---

## 🛠 Tech Stack

| Layer        | Technology                        |
|-------------|-----------------------------------|
| Backend      | Java 21, Spring Boot 3.2          |
| Web          | Spring MVC, Thymeleaf             |
| Security     | Spring Security 6, BCrypt         |
| Persistence  | Spring Data JPA, Hibernate        |
| Database     | MySQL 8.x                         |
| Build        | Maven 3.9+                        |
| AI           | Google Gemini REST API (Java HTTP)|
| Charts       | Chart.js 4.4                      |
| CSS/JS       | Vanilla CSS3 + ES6 JavaScript     |

---

## 📦 Prerequisites

1. **Java 21** — [Download](https://adoptium.net/)
2. **Maven 3.9+** — [Download](https://maven.apache.org/)
3. **MySQL 8.x** — [Download](https://dev.mysql.com/downloads/)

---

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <repo-url>
cd FIX-flow
```

### 2. Create the MySQL Database
```sql
CREATE DATABASE fixflow_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'fixflow'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON fixflow_db.* TO 'fixflow'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure Application Properties
Copy the example:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/fixflow_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=fixflow
spring.datasource.password=your_password
```

Or use environment variables:
```bash
# Windows (PowerShell)
$env:DB_USERNAME="fixflow"
$env:DB_PASSWORD="your_password"

# Linux/Mac
export DB_USERNAME=fixflow
export DB_PASSWORD=your_password
```

### 4. (Optional) Configure Gemini AI
```bash
# Windows
$env:GEMINI_API_KEY="your_gemini_api_key"

# Linux/Mac
export GEMINI_API_KEY=your_gemini_api_key
```
> The application works without this. AI features will show an "unavailable" message.

### 5. Build and Run
```bash
mvn clean spring-boot:run
```

The application will:
- Create/update the database schema automatically (Hibernate DDL)
- Seed sample data on first run
- Start at **http://localhost:8080**

---

## 👥 Demo Credentials

| Role       | Email                         | Password     |
|-----------|-------------------------------|--------------|
| Admin      | admin@fixflow.com             | admin123     |
| Staff      | ravi@fixflow.com              | staff123     |
| Staff      | priya@fixflow.com             | staff123     |
| Student    | arjun@student.fixflow.com     | student123   |
| Student    | sneha@student.fixflow.com     | student123   |

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/fixflow/
│   │   ├── FixFlowApplication.java
│   │   ├── ai/               ← GeminiService
│   │   ├── config/           ← SecurityConfig, MvcConfig, SampleDataInitializer
│   │   ├── controller/       ← AuthController, StudentController, AdminController, StaffController, AiController
│   │   ├── dto/              ← RegisterDto, ComplaintDto, DashboardStatsDto, AiRequestDto
│   │   ├── entity/           ← User, Department, Complaint, ComplaintHistory + Enums
│   │   ├── exception/        ← GlobalExceptionHandler
│   │   ├── repository/       ← JPA Repositories
│   │   ├── scheduler/        ← OverdueDetectionScheduler
│   │   ├── security/         ← CustomUserDetailsService, CustomAuthSuccessHandler
│   │   └── service/          ← UserService, ComplaintService
│   └── resources/
│       ├── static/
│       │   ├── css/app.css
│       │   └── js/app.js
│       ├── templates/
│       │   ├── auth/         ← login.html, register.html
│       │   ├── student/      ← dashboard, my-complaints, submit-complaint, complaint-detail
│       │   ├── admin/        ← dashboard, complaints, complaint-detail, overdue, departments
│       │   ├── staff/        ← dashboard, complaint-detail
│       │   ├── ai/           ← assistant.html
│       │   ├── error/        ← 400, 403, 404, 500
│       │   └── fragments/    ← layout.html (sidebar, topbar, badges)
│       └── application.properties
```

---

## 🌍 Environment Variables

| Variable               | Default                                            | Description                                     |
|------------------------|----------------------------------------------------|-------------------------------------------------|
| `DB_URL`               | jdbc:mysql://localhost:3306/fixflow_db...          | MySQL JDBC connection URL                       |
| `DB_USERNAME`          | root                                               | MySQL username                                  |
| `DB_PASSWORD`          | (empty)                                            | MySQL password                                  |
| `GEMINI_API_KEY`       | (empty)                                            | Google Gemini API key (optional)                |
| `GEMINI_MODEL`         | gemini-3.5-flash                                   | Gemini text-generation model                    |
| `APP_INIT_SAMPLE_DATA` | true                                               | Seed sample users/data (set to false in prod)   |

---

## 🏗 Architecture

```
Browser (Thymeleaf)
      │
      ▼
Spring MVC Controllers  ←→  Spring Security (RBAC)
      │
      ▼
Service Layer (Business Logic)
      │        │
      ▼        ▼
JPA Repos    GeminiService (Java HttpClient)
      │             │
      ▼             ▼
MySQL DB      Gemini REST API
```

**Key Design Decisions:**
- Thymeleaf server-side rendering — no separate frontend build step
- Spring Security method-level `@PreAuthorize` + URL patterns for RBAC
- BCrypt password hashing (strength 12)
- Complaint ID generation is synchronized to avoid duplicates
- Duplicate detection uses keyword similarity (Jaccard-like) — no AI required
- Overdue detection runs every 30 minutes via Spring `@Scheduled`
- Gemini API called via Java 21 `HttpClient` — no SDK dependency
- AI output treated as plain text, never rendered as HTML (XSS-safe)

---

## ⚠ Known Limitations

1. **Email notifications** — Not implemented. Only in-app status tracking.
2. **Real-time updates** — No WebSocket. Page refresh required for updates.
3. **Pagination** — Not implemented for large datasets (suitable for demo scale).
4. **Password reset** — Not implemented.
5. **Staff self-registration** — Staff accounts are created by the sample data initializer. Admin UI for creating staff not yet implemented.
6. **Gemini rate limits** — Free tier has request limits; errors are handled gracefully.
7. **File storage** — Images stored locally in `uploads/` folder. Not suitable for production cloud deployment without modification.

---

## 🧪 Testing Workflow

1. Log in as **Student** → Submit a complaint → Check My Complaints
2. Log in as **Admin** → Assign the complaint to Staff → Update status
3. Log in as **Staff** → View assigned complaint → Mark as IN_PROGRESS / RESOLVED
4. Log in as **Admin** → View Dashboard charts and overdue complaints
5. Visit AI Assistant (any role) — test all 5 AI tabs

---

## 📄 License

MIT License — Free for educational and personal use.
