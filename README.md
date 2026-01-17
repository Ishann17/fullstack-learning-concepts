# Spring Boot User Service

> A production-grade Spring Boot microservice demonstrating scalable data processing, optimized database operations, and enterprise patterns for handling millions of records.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technical Highlights](#technical-highlights)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Performance Optimizations](#performance-optimizations)
- [Lessons Learned](#lessons-learned)
- [Tech Stack](#tech-stack)
- [Future Roadmap](#future-roadmap)

---

## 🎯 Overview

This project goes beyond basic CRUD operations to solve **real-world backend engineering challenges** that product companies face when dealing with large-scale data processing. Built with a focus on performance, scalability, and production-ready patterns.

### What Makes This Different?

- **High-volume data processing**: Successfully handles 1M–3M+ user records
- **Production patterns**: Clean architecture, proper error handling, observability
- **Performance engineering**: Solved real bottlenecks like OFFSET pagination slowness and Hibernate batching issues
- **Memory optimization**: Streaming exports without RAM spikes

---

## ✨ Key Features

### 🔄 External API Integration
- Integration with [RandomUser.me API](https://randomuser.me/api/) for realistic test data
- Migrated from `RestTemplate` to reactive `WebClient` for better performance
- Custom buffer configuration (10MB) to handle large API responses
- Nationality filtering for data consistency (`nat=us,ca,au,gb,in`)

### 💾 Optimized Data Import
- **Bulk insert** with intelligent batching (1000 records/batch)
- Real-time progress logging with metrics:
  - Batch number and inserted count
  - Percentage completion
  - Processing speed (users/sec)
  - Elapsed time
- **Transactional resilience**: Batch-level commits prevent total rollback on failures
- Fixed Hibernate batching by switching from `IDENTITY` to `TABLE` ID generation strategy

### 📤 Dual Export Strategies

#### File-Based Export
Simple CSV generation for smaller datasets with downloadable file response.

#### Streaming Export (Production-Ready)
- **Memory-safe streaming** using `StreamingResponseBody`
- Handles millions of rows without memory overflow
- **Keyset pagination** instead of OFFSET for consistent performance
- Buffered writing with per-batch flushing
- Successfully validated with 3M+ record exports

### 🔍 Dynamic Search
Flexible filtering API using Spring Data Specifications:
- Filter by: name, city, state, age
- Mimics real-world search functionality (similar to e-commerce/ride-sharing apps)

### 🛡️ Global Exception Handling
Centralized error handling with custom exceptions:
- `UserNotFoundException`
- `BatchLimitExceededException`
- Consistent error response format across all endpoints

---

## 🏗️ Architecture

```
┌─────────────────┐
│   Controller    │  ← REST endpoints
└────────┬────────┘
         │
┌────────▼────────┐
│    Service      │  ← Business logic
└────────┬────────┘
         │
┌────────▼────────┐
│   Repository    │  ← Data access (JPA)
└────────┬────────┘
         │
┌────────▼────────┐
│     MySQL       │  ← Persistence layer
└─────────────────┘
```

**Clean separation of concerns:**
- **Controller Layer**: HTTP request handling, input validation
- **Service Layer**: Business logic, orchestration
- **Repository Layer**: Database operations
- **DTO/Mapper Pattern**: Clean API contracts, entity protection

---

## 🚀 Technical Highlights

### Database Performance Tuning

**Problem**: Hibernate batch inserts weren't working  
**Root Cause**: `GenerationType.IDENTITY` prevents batch optimization  
**Solution**: Switched to `GenerationType.TABLE` with optimized allocation size

### Streaming Export Optimization

**Problem**: Traditional OFFSET pagination slows down with large datasets
```sql
-- Slow for large offsets
SELECT * FROM user LIMIT 1000 OFFSET 2000000;
```

**Solution**: Keyset (cursor-based) pagination
```sql
-- Fast and consistent
SELECT * FROM user WHERE id > :lastId ORDER BY id ASC LIMIT 1000;
```

**Impact**: Consistent performance regardless of dataset size

### Transaction Management

**Problem**: Mid-process failures caused complete rollback of large imports  
**Solution**: 
- Batch-level transactions with `REQUIRES_NEW` propagation
- Separate service bean (`UserBatchSaverService`) to avoid Spring proxy limitations
- Partial progress persistence for long-running operations

### Memory Management

**Techniques Used**:
- `flush()` and `clear()` after each batch to release EntityManager memory
- `Slice` instead of `Page` to avoid expensive `COUNT(*)` queries
- Buffered I/O with controlled flush intervals for streaming exports

---

## 🏁 Getting Started

### Prerequisites
- Java 17 or higher
- MySQL 8.0+
- Maven 3.6+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/Ishann17/springboot-learning-concepts
cd spring-boot-user-service
```

2. **Configure database**
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/userdb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. **Build the project**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

---

## 📡 API Endpoints

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users` | Get all users (paginated) |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/api/users/search` | Search with filters |
| `POST` | `/api/users` | Create new user |
| `PUT` | `/api/users/{id}` | Update user |
| `DELETE` | `/api/users/{id}` | Delete user |

### Data Import/Export

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/import/random-users` | Import from RandomUser API |
| `POST` | `/api/import/bulk` | Bulk insert with batching |
| `GET` | `/api/export/csv` | Download CSV file |
| `GET` | `/api/export/stream` | Stream CSV (for large datasets) |

### Search Example
```bash
GET /api/users/search?name=John&city=Austin&minAge=25&maxAge=35
```

---

## ⚡ Performance Optimizations

### Import Performance
- **Batch size**: 1000 records per transaction
- **Throughput**: ~5,000-10,000 users/second (hardware dependent)
- **Memory**: Constant memory usage via `flush()`/`clear()` strategy

### Export Performance
- **Dataset**: Successfully exported 3,000,000 users
- **Memory footprint**: Minimal (streaming approach)
- **Pagination**: Keyset-based for consistent speed

### Database Optimizations
- Proper indexing on frequently queried columns
- Batch insert optimization with `TABLE` generation strategy
- Query optimization using `Slice` to avoid count queries

📊 Performance Results (Measured on Local Setup)

✅ CSV Export (Streaming + Keyset Pagination)
- Records Exported: 3,000,000 users
- Approach: StreamingResponseBody + BufferedWriter + Keyset Pagination
- Time Taken: ~12 seconds
- RAM Usage: Stable (no major spikes) ✅
- Notes: Excel can’t open full file due to 1,048,576 row limit, file verified using CLI.

✅ Bulk Import (Faker + Batch Insert + Batch Commits)
- Records Imported: 2,000,000 users
- Batch Size: 1000
- Transaction Strategy: REQUIRES_NEW per batch commit
- DB Insert Time: ~281 seconds
- Avg Insert Speed: ~7100 users/sec

- Benefit: Partial progress persists even if the app/network fails mid-way ✅

---

## 📚 Lessons Learned

### 1. Hibernate Batch Insert Gotcha
`GenerationType.IDENTITY` disables Hibernate batching because the database must return generated IDs immediately. Using `TABLE` or `SEQUENCE` strategies enables true batch inserts.

### 2. Spring Transaction Proxy Limitations
Calling `@Transactional` methods from within the same class doesn't work due to Spring's proxy mechanism. Solution: Extract batch processing to a separate service bean.

### 3. OFFSET vs Keyset Pagination
OFFSET becomes exponentially slower with large datasets. Keyset pagination maintains consistent performance by using indexed cursor traversal.

### 4. WebClient Buffer Limits
Default WebClient in-memory buffer is ~256KB. Large API responses require custom configuration with increased limits.

### 5. CSV Export at Scale
Traditional `findAll()` approaches cause memory exhaustion. Streaming with `StreamingResponseBody` and pagination enables processing of unlimited dataset sizes.

---

## 🛠️ Tech Stack

**Backend Framework**
- Spring Boot 3.x
- Spring Data JPA (Hibernate)
- Spring WebFlux (WebClient)

**Database**
- MySQL 8.0

**Data Processing**
- Java Faker (test data generation)

**Developer Tools**
- Lombok (boilerplate reduction)
- Maven (dependency management)

---

## 🗺️ Future Roadmap

- [ ] **Async Job Processing**: Job-based import/export with status polling
- [ ] **Compression**: Gzip-compressed CSV streaming
- [ ] **Filtered Exports**: Apply search criteria to streaming exports
- [ ] **Rate Limiting**: Protect heavy endpoints from abuse
- [ ] **Metrics & Monitoring**: Integration with Prometheus/Grafana
- [ ] **Caching Layer**: Redis integration for frequently accessed data
- [ ] **API Documentation**: Swagger/OpenAPI integration
- [ ] **Containerization**: Docker support with docker-compose

---

## 👨‍💻 Author

**Ishan**

Built with ☕ and ❤️ as a learning project to master backend engineering fundamentals.

---

## ⭐ Show Your Support

If this project helped you learn something new, please consider giving it a ⭐!

---

<div align="center">
Made with passion for backend engineering excellence
</div>
