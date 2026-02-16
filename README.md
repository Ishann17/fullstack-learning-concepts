# Spring Boot User Service

> A production-grade Spring Boot microservice demonstrating scalable data processing, distributed rate limiting, optimized database operations, and enterprise patterns for handling millions of records.

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red.svg)](https://redis.io/)

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

This project goes beyond basic CRUD operations to solve **real-world backend engineering challenges** that product companies face when dealing with large-scale data processing, distributed systems, and high-concurrency scenarios. Built with a focus on performance, scalability, and production-ready patterns.

### What Makes This Different?

- **High-volume data processing**: Successfully handles 1M–5M+ user records
- **Distributed rate limiting**: Redis-based concurrency control for multi-server deployments
- **Production patterns**: Clean architecture, proper error handling, atomic operations
- **Performance engineering**: Solved real bottlenecks like OFFSET pagination slowness and Hibernate batching issues
- **Memory optimization**: Streaming exports without RAM spikes
- **Async job processing**: Non-blocking operations with real-time status tracking

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

### ⚡ Distributed Rate Limiting (Production-Ready)
- **Redis-based rate limiting** for multi-server deployments
- **Tier-based concurrency control**: 
  - SMALL jobs (≤100 users): 10 concurrent
  - MEDIUM jobs (≤10K users): 5 concurrent  
  - LARGE jobs (≤100K users): 3 concurrent
  - XL jobs (>100K users): 1 concurrent + 30s cooldown
- **Atomic operations** using Lua scripts to prevent race conditions
- **Self-healing**: TTL-based cleanup prevents orphaned state from app crashes
- **Non-blocking**: Replaced O(N) `KEYS` scan with O(1) Redis SET operations
- **Per-user fairness**: Independent concurrency tracking per user per tier

### 🔄 Async Job Processing
- Non-blocking import operations with job tracking
- Real-time job status monitoring (PENDING → IN_PROGRESS → COMPLETED/FAILED)
- Progress percentage updates during long-running imports
- Resilient to application crashes using Redis-backed state
- Supports concurrent job execution within rate limits

### 🔍 Dynamic Search
Flexible filtering API using Spring Data Specifications:
- Filter by: name, city, state, age
- Mimics real-world search functionality (similar to e-commerce/ride-sharing apps)

### 🛡️ Global Exception Handling
Centralized error handling with custom exceptions:
- `UserNotFoundException`
- `BatchLimitExceededException`
- `TooManyRequestsException` (HTTP 429)
- `CooldownActiveException`
- Consistent error response format across all endpoints

---

## 🏗️ Architecture
```
┌──────────────────────────────────────────────────────────────┐
│                     Client/Frontend                           │
└───────────────────────────┬──────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│                    Controller Layer                           │
│  • REST Endpoints  • Input Validation  • Job Initiation      │
└───────────────────────────┬──────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
┌───────▼──────┐   ┌────────▼────────┐   ┌────▼─────────┐
│   Service    │   │  Rate Limiter   │   │  Job Tracker │
│    Layer     │   │   (Redis Lua)   │   │   (Redis)    │
└───────┬──────┘   └─────────────────┘   └──────────────┘
        │
┌───────▼──────────────────────────────────────────────────────┐
│              Async Processing Layer (@Async)                  │
│  • Background Jobs  • Batch Processing  • Progress Updates   │
└───────┬──────────────────────────────────────────────────────┘
        │
┌───────▼──────┐                    ┌──────────────────┐
│  Repository  │◄───────────────────┤  Redis (State)   │
│   (JPA/ORM)  │                    │  • Rate Limits   │
└───────┬──────┘                    │  • Job Status    │
        │                           │  • Running Jobs  │
┌───────▼──────┐                    └──────────────────┘
│    MySQL     │
│ (Persistent) │
└──────────────┘
```

**Key Design Decisions:**
- **Controller Layer**: Orchestrates requests, enforces rate limits before job acceptance
- **Service Layer**: Business logic, batch coordination
- **Redis Layer**: Distributed state management (rate limits, job tracking)
- **Async Layer**: Decouples heavy processing from HTTP request lifecycle
- **Repository Layer**: Database operations with optimized batching

---

## 🚀 Technical Highlights

### Distributed Rate Limiting Architecture

**Problem**: Prevent resource exhaustion when multiple users trigger expensive bulk operations simultaneously across multiple server instances.

**Challenges Solved**:
1. **Race Conditions**: Multiple servers checking limits at the same time
2. **Scalability**: In-memory state doesn't work across multiple instances
3. **Fairness**: Per-user, per-tier concurrency enforcement
4. **Crash Recovery**: Orphaned jobs blocking future requests

**Solution Architecture**:

**Key Components**:

1. **Redis SET for Job Tracking**
```
   Key: user:ishan:XL:jobs
   Value: SET {job-001, job-002, job-003}
   Benefit: O(1) counting via SCARD (vs O(N) KEYS scan)
```

2. **Lua Script for Atomicity**
```lua
   -- Atomic check + reserve operation
   local current = redis.call('SCARD', KEYS[1])
   if current >= limit then return 0 end
   redis.call('SADD', KEYS[1], jobId)
   return 1
```
   **Why Lua?**: Executes as single atomic operation inside Redis, preventing race conditions when multiple requests arrive simultaneously.

3. **TTL Safety Keys**
```
   Key: job:ishan:XL:job-abc
   TTL: 15 minutes
   Purpose: Auto-cleanup if app crashes before calling markJobFinished()
```

**Impact**:
- ✅ Zero race conditions under concurrent load
- ✅ Works across multiple server instances (horizontally scalable)
- ✅ Self-healing (orphaned jobs auto-expire via TTL)
- ✅ Fair resource allocation per user per tier

---

### Database Performance Tuning

**Problem**: Hibernate batch inserts weren't working despite configuration  
**Root Cause**: `GenerationType.IDENTITY` prevents batch optimization (DB must return IDs immediately)  
**Solution**: Switched to `GenerationType.TABLE` with optimized allocation size (1000)

**Impact**: Enabled true JDBC batching, reducing database round-trips from 2M to 2K for 2M records.

---

### Streaming Export Optimization

**Problem**: Traditional OFFSET pagination slows down exponentially with large datasets
```sql
-- Slow for large offsets (scans + skips millions of rows)
SELECT * FROM user LIMIT 1000 OFFSET 2000000;
```

**Solution**: Keyset (cursor-based) pagination
```sql
-- Fast and consistent (uses index directly)
SELECT * FROM user WHERE id > :lastId ORDER BY id ASC LIMIT 1000;
```

**Impact**: Consistent sub-millisecond query performance regardless of dataset size (tested up to 3M records).

---

### Transaction Isolation for Partial Success

**Real-World Scenario**: Importing 500 batches (500,000 users), batch #347 fails due to network issue.

**Old Approach** (Single Transaction):
```java
@Transactional  // All or nothing
public void importUsers(List<User> users) {
    for (Batch batch : batches) {
        saveBatch(batch);  // If ANY fails → ALL rollback ❌
    }
}
// Result: 0 users saved after 15 minutes of processing
```

**New Approach** (Independent Batch Transactions):
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveOneBatch(List<User> batch) {
    repository.saveAll(batch);
    entityManager.flush();   // Force SQL execution
    entityManager.clear();   // Free memory
}
// Each batch commits independently
// Result: 346,000 users saved, only 1,000 lost ✅
```

**Why Separate Service?**  
Spring's `@Transactional` uses AOP proxies. Self-invocation bypasses the proxy, breaking transaction semantics. Solution: Extract to `UserBatchSaverService` bean.

**Business Value**: Long-running imports can partially succeed, avoiding complete data loss on failures.

---

### Memory Management

**Techniques Used**:
- `flush()` and `clear()` after each batch to release EntityManager memory
- `Slice` instead of `Page` to avoid expensive `COUNT(*)` queries
- Buffered I/O with controlled flush intervals for streaming exports
- Prevented OutOfMemoryError during multi-million record processing

---

## 🏁 Getting Started

### Prerequisites
- Java 17 or higher
- MySQL 8.0+
- Redis 7.x
- Maven 3.6+
- Docker (optional, for Redis)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/Ishann17/springboot-learning-concepts
cd spring-boot-user-service
```

2. **Start Redis (using Docker)**
```bash
docker run -d --name redis-server -p 6379:6379 redis:latest
```

3. **Configure database**
```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/userdb
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

4. **Build the project**
```bash
mvn clean install
```

5. **Run the application**
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

### Async Job Management

| Method | Endpoint | Description | Headers |
|--------|----------|-------------|---------|
| `POST` | `/api/v1/users/import/async` | Start async import job | `X-USER-ID: {userId}` |
| `GET` | `/api/v1/jobs/{jobId}` | Get job status | - |

---

### API Examples

#### Search Users
```bash
GET /api/users/search?name=John&city=Austin&minAge=25&maxAge=35
```

#### Start Async Import
```bash
curl -X POST "http://localhost:8080/api/v1/users/import/async?count=100000" \
  -H "X-USER-ID: user123"
```

**Response:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Import started. Use jobId to check status."
}
```

#### Check Job Status
```bash
GET /api/v1/jobs/550e8400-e29b-41d4-a716-446655440000
```

**Response (In Progress):**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "IN_PROGRESS",
  "requestedCount": 100000,
  "processedCount": 45000,
  "progress": 45,
  "startedAt": "2024-02-16T10:00:00",
  "message": "Processing batch 45/100..."
}
```

#### Rate Limit Response (429 Too Many Requests)
```json
{
  "timestamp": "2024-02-16T10:05:30",
  "status": 429,
  "error": "Too Many Requests",
  "message": "XL concurrency limit reached. Max allowed = 1"
}
```

---

## ⚡ Performance Optimizations

### Import Performance
- **Batch size**: 1000 records per transaction
- **Throughput**: ~7,000 users/second (hardware dependent)
- **Memory**: Constant memory usage via `flush()`/`clear()` strategy
- **Transaction strategy**: Independent batch commits for failure resilience

### Export Performance
- **Dataset**: Successfully exported 3,000,000 users
- **Time**: ~12 seconds (streaming approach)
- **Memory footprint**: Minimal (streaming + buffering)
- **Pagination**: Keyset-based for consistent speed

### Database Optimizations
- Proper indexing on frequently queried columns
- Batch insert optimization with `TABLE` generation strategy
- Query optimization using `Slice` to avoid count queries
- Connection pooling with HikariCP

### Rate Limiting Performance

**Challenge**: Traditional `KEYS` pattern matching blocks Redis

**Problem**:
```java
// ❌ BAD: O(N) operation, scans ALL keys, blocks Redis
Set<String> keys = redis.keys("job:ishan:XL:*");
return keys.size();
```

**Solution**:
```java
// ✅ GOOD: O(1) operation, non-blocking
Long size = redis.opsForSet().size("user:ishan:XL:jobs");
```

**Impact**:
- **Old approach**: Scan 1M keys = ~100-500ms (blocks all Redis operations!)
- **New approach**: Instant SCARD = <1ms ✅
- **Scalability**: Performance stays constant as data grows

---

## 📊 Performance Results (Measured on Local Setup)

### ✅ CSV Export (Streaming + Keyset Pagination)
- **Records Exported**: 3,000,000 users
- **Approach**: StreamingResponseBody + BufferedWriter + Keyset Pagination
- **Time Taken**: ~12 seconds
- **RAM Usage**: Stable (no major spikes) ✅
- **Notes**: Excel can't open full file due to 1,048,576 row limit, file verified using CLI

### ✅ Bulk Import (Faker + Batch Insert + Batch Commits)
- **Records Imported**: 2,000,000 users
- **Batch Size**: 1000 records/batch
- **Transaction Strategy**: REQUIRES_NEW (independent batch commits)
- **DB Insert Time**: ~281 seconds
- **Avg Insert Speed**: ~7,100 users/sec
- **Benefit**: Partial progress persists even if app/network fails mid-way ✅

### ✅ Rate Limiting Under Load
- **Concurrent Requests**: 100 simultaneous requests
- **Response Time**: <5ms for rate limit check
- **Accuracy**: 100% (zero over-bookings)
- **Redis Operations**: All sub-millisecond (SCARD, SADD, Lua execution)

---

## 📚 Lessons Learned

### 1. Hibernate Batch Insert Gotcha
`GenerationType.IDENTITY` disables Hibernate batching because the database must return generated IDs immediately. Using `TABLE` or `SEQUENCE` strategies enables true batch inserts.

### 2. Spring Transaction Proxy Limitations
Calling `@Transactional` methods from within the same class doesn't work due to Spring's proxy mechanism. Solution: Extract batch processing to a separate service bean.

### 3. OFFSET vs Keyset Pagination
OFFSET becomes exponentially slower with large datasets (database must scan and skip rows). Keyset pagination maintains consistent performance by using indexed cursor traversal.

### 4. WebClient Buffer Limits
Default WebClient in-memory buffer is ~256KB. Large API responses require custom configuration with increased limits (`maxInMemorySize`).

### 5. CSV Export at Scale
Traditional `findAll()` approaches cause memory exhaustion. Streaming with `StreamingResponseBody` and pagination enables processing of unlimited dataset sizes.

### 6. Redis KEYS vs SCAN/SET
Using `KEYS` pattern matching in production is dangerous—it's an O(N) operation that blocks Redis. For production, use Redis SET with `SCARD` (O(1)) or `SCAN` for non-blocking iteration.

### 7. Lua Scripts for Atomic Guarantees
When multiple operations must execute atomically (check + reserve), Lua scripts executed inside Redis provide transaction-like guarantees without network round trips between operations.

### 8. TTL as a Safety Net
In distributed systems, explicit cleanup (like `finally` blocks) can fail due to crashes. TTL (Time To Live) acts as a self-healing mechanism, automatically removing orphaned state.

### 9. Spring @Async Proxy Behavior
`@Async` methods must be in a separate Spring bean and called from external classes. Self-invocation bypasses Spring's proxy, causing methods to run synchronously on the calling thread.

---

## 🛠️ Tech Stack

**Backend Framework**
- Spring Boot 3.x
- Spring Data JPA (Hibernate)
- Spring WebFlux (WebClient)
- Spring Async (`@Async`)

**Databases**
- MySQL 8.0 (Persistent storage)
- Redis 7.x (Distributed state, rate limiting, job tracking)

**Data Processing**
- Java Faker (Test data generation)
- Lua (Atomic Redis scripts)

**Design Patterns**
- Repository Pattern
- DTO/Mapper Pattern
- Strategy Pattern (Tier-based rate limiting)
- Saga Pattern (Independent batch transactions)

**Developer Tools**
- Lombok (Boilerplate reduction)
- Maven (Dependency management)
- Docker (Redis containerization)

---

## 🗺️ Future Roadmap

### ✅ Completed
- [x] Async Job Processing with real-time status
- [x] Redis-based Distributed Rate Limiting
- [x] Job Status Tracking with progress updates
- [x] Atomic Operations using Lua Scripts
- [x] Tier-based Concurrency Control
- [x] Self-healing with TTL-based cleanup

### 🚧 In Progress
- [ ] **Ghost Job Cleanup**: Redis keyspace notifications or scheduled cleanup job
- [ ] **Comprehensive Testing**: Unit, integration, and load tests with >70% coverage
- [ ] **API Documentation**: Swagger/OpenAPI integration

### 📋 Planned
- [ ] **Enhanced Observability**:
  - Prometheus metrics export
  - Grafana dashboards for real-time monitoring
  - Distributed tracing (Sleuth/Zipkin)
  - Custom metrics for job throughput and latency
  
- [ ] **Production Hardening**:
  - Circuit breaker for external API calls (Resilience4j)
  - Retry logic with exponential backoff
  - Idempotency keys for duplicate prevention
  - Health check endpoints
  
- [ ] **DevOps & Infrastructure**:
  - Docker Compose for local development environment
  - Kubernetes manifests for production deployment
  - CI/CD pipeline (GitHub Actions)
  - Infrastructure as Code (Terraform/Helm)
  
- [ ] **Advanced Features**:
  - Webhook notifications on job completion
  - Admin dashboard (React/Vue) for job monitoring
  - Multi-format export (JSON, Parquet, Avro)
  - Filtered streaming exports
  - Data deduplication strategies
  - Compression support (Gzip) for exports

---

## 👨‍💻 Author

**Ishan**

Built with ☕ and ❤️ to master production-grade backend engineering fundamentals including distributed systems, concurrency control, performance optimization, and scalable architecture patterns.

---

## ⭐ Show Your Support

If this project helped you learn something new about building scalable backend systems, please consider giving it a ⭐!

---

<div align="center">
Made with passion for backend engineering excellence
</div>
