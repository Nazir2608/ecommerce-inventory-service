# 🚀 Ecommerce Inventory Service

Production-ready **modular monolith** designed to handle **Myntra-style flash sale scenarios** with strict overselling protection, high concurrency safety, and scalable system design.

This project focuses on real distributed system challenges — not basic CRUD.

---

## 🎯 Problem Statement

During a flash sale:

- 10,000 users click **“Buy Now”**
- Only 100 items are available
- All users read `stock = 100`
- Without proper concurrency control → overselling happens

Overselling is **not a Java bug.**  
It is a **system design problem under concurrency.**

This system solves that correctly using layered protection.

---

## 🏗 High-Level Architecture

Single deployable JAR  
Stateless service  
Internally modular

```
                 ┌─────────────────────┐
                 │        Client        │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │    Load Balancer     │
                 └──────────┬──────────┘
                            ↓
                 ┌─────────────────────┐
                 │ Inventory Application│
                 │  (Spring Boot)       │
                 │----------------------│
                 │  product module      │
                 │  reservation module  │
                 │  stock module        │
                 │  flashsale module    │
                 │  scheduler module    │
                 │  event module        │
                 └──────────┬──────────┘
                            ↓
         ┌──────────────────┼──────────────────┐
         ↓                  ↓                  ↓
     PostgreSQL           Redis              Kafka
  (Source of Truth)   (Flash Guard)    (Event-Ready)
```

---

## 🧠 Core Design Principles

### 1️⃣ Reservation-Based Inventory Model

Stock is never deducted directly.

Instead, we use a lifecycle:

```
RESERVED → CONFIRMED
RESERVED → RELEASED
RESERVED → EXPIRED
```

This supports:

- Payment delay
- Timeout handling
- Retry safety
- Idempotency

---

### 2️⃣ Layered Overselling Protection

| Layer | Purpose |
|--------|----------|
| Redis | Fast rejection during spikes |
| Atomic SQL | Hard consistency guarantee |
| Idempotency | Prevent duplicate reservations |
| Scheduler | Cleanup expired reservations |
| (Optional) Queue | Throttle extreme traffic |

The database is the **last line of defense.**

---

### 3️⃣ Atomic Database Protection (Critical)

Overselling prevention is handled using atomic SQL:

```sql
UPDATE product
SET reserved_stock = reserved_stock + :qty
WHERE id = :productId
AND (total_stock - reserved_stock) >= :qty;
```

If rows affected = 0 → reject request.

This guarantees:

- No negative stock
- Safe under concurrency
- Works across multiple application instances

---

### 4️⃣ Flash Sale Mode (Redis Optimization)

During high traffic:

1. Redis `DECR` pre-check
2. Reject immediately if stock exhausted
3. Fallback to DB atomic update
4. Compensate Redis if DB fails

Redis protects the database from 10K+ RPS spikes.

---

## 🔄 Core Reservation Flow

```
User → API → FlashSaleService
               ↓
         Redis DECR (if enabled)
               ↓
         StockService (Atomic DB Update)
               ↓
         Create Reservation
               ↓
         Return RESERVED
```

If payment succeeds:

```
RESERVED → CONFIRMED
```

If payment fails or timeout:

```
RESERVED → RELEASED / EXPIRED
```

---

## 📦 Module Structure

```
com.nazir.inventory
├── product
├── reservation
├── stock
├── flashsale
├── scheduler
├── event
└── common
```

---

## 🗄 Database Schema

### product

- id
- name
- total_stock
- reserved_stock
- version
- created_at

### reservation

- id
- order_id (unique)
- product_id
- quantity
- status
- expires_at
- created_at

Index:

```
(status, expires_at)
```

---

## 📊 APIs

- **POST** `/api/v1/products`
- **GET** `/api/v1/products/{id}`
- **POST** `/api/v1/reservations`
- **POST** `/api/v1/reservations/{orderId}/confirm`
- **POST** `/api/v1/reservations/{orderId}/release`

---

## 🧪 Testing Strategy

### Unit Tests

- Successful reservation
- Out-of-stock scenario
- Idempotency validation

### Integration Tests

- Atomic DB safety
- Reservation lifecycle
- Scheduler expiration

### Concurrency Test

Simulate:

- 100 threads
- 10 stock

Expected:

- Only 10 successful reservations
- Stock never goes negative

---

## ⚙️ Tech Stack

- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka
- Docker
- Actuator
- OpenAPI (Swagger)

---

## 🐳 Run Locally

```bash
docker-compose up -d
mvn clean install
mvn spring-boot:run
```

Swagger UI:

```
http://localhost:8080/swagger-ui.html
```
## 👨‍💻 Author

Nazir  
Backend Engineer | Distributed Systems Enthusiast
