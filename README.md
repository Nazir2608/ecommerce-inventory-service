#  Ecommerce Inventory Service

A production-ready **modular monolith** built to handle **Myntra-style flash sale scenarios** — with strict overselling protection, high-concurrency safety, and scalable system design at its core.

---

## 📌 Table of Contents

- [Problem Statement](#-problem-statement)
- [High-Level Architecture](#-high-level-architecture)
- [Core Design Principles](#-core-design-principles)
- [Reservation Lifecycle](#-reservation-lifecycle)
- [Core Reservation Flow](#-core-reservation-flow)
- [Module Structure](#-module-structure)
- [Database Schema](#-database-schema)
- [APIs](#-apis)
- [Testing Strategy](#-testing-strategy)
- [Tech Stack](#-tech-stack)
- [Run Locally](#-run-locally)

---

## 🎯 Problem Statement

During a flash sale:

- 10,000 users click **"Buy Now"** simultaneously
- Only **100 items** are available
- All users read `stock = 100` at the same time
- Without proper concurrency control → **overselling happens**

Overselling is **not a Java bug.**
It is a **system design problem under concurrency.**

This system solves it correctly using layered, defense-in-depth protection.

---

## 🏗 High-Level Architecture

A single deployable JAR — stateless service, internally modular.

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

**Each infrastructure component has a clear role:**

| Component | Role |
|-----------|------|
| PostgreSQL | Source of truth — atomic stock enforcement |
| Redis | Fast pre-check layer — absorbs traffic spikes |
| Kafka | Event publishing — decoupled downstream processing |

---

##  Core Design Principles

### 1️⃣ Reservation-Based Inventory Model

Stock is **never deducted directly.** Instead, a reservation lifecycle is used:

```
RESERVED → CONFIRMED    (payment succeeded)
RESERVED → RELEASED     (payment failed / user cancelled)
RESERVED → EXPIRED      (timeout — scheduler cleans up)
```

This model provides:

- **Payment delay tolerance** — stock is held while payment processes
- **Timeout handling** — expired reservations are automatically released
- **Retry safety** — re-submitting the same order is safe
- **Idempotency** — duplicate requests are detected and rejected

---

### 2️⃣ Layered Overselling Protection

No single layer is trusted alone. Each layer adds a line of defense:

| Layer | Mechanism | Purpose |
|-------|-----------|---------|
| **Redis** | `DECR` pre-check | Fast rejection during traffic spikes |
| **Atomic SQL** | `UPDATE ... WHERE stock >= qty` | Hard consistency guarantee |
| **Idempotency** | Unique `order_id` constraint | Prevent duplicate reservations |
| **Scheduler** | Periodic expiry scan | Release stuck/abandoned reservations |
| **Queue** *(optional)* | Request buffering | Throttle extreme traffic beyond Redis |

> The **database is the last line of defense** — it always wins.

---

### 3️⃣ Atomic Database Protection

Overselling prevention at the DB layer uses a single atomic SQL statement:

```sql
UPDATE product
SET reserved_stock = reserved_stock + :qty
WHERE id = :productId
AND (total_stock - reserved_stock) >= :qty;
```

**If `rows affected = 0` → request is rejected immediately.**

This guarantees:

- ✅ Stock never goes negative
- ✅ Safe under high concurrency
- ✅ Works correctly across multiple application instances
- ✅ No need for application-level locking

---

### 4️⃣ Flash Sale Mode (Redis Optimization)

When flash sale mode is active, Redis acts as a fast gate to protect the database from 10K+ RPS spikes:

```
1. Incoming request hits Redis DECR
2. If Redis counter ≤ 0 → reject immediately (no DB hit)
3. If Redis allows → proceed to atomic DB update
4. If DB update fails → compensate by incrementing Redis counter back
```

**Redis is the speed layer. PostgreSQL is the safety layer. Both are required.**

---

## 🔄 Reservation Lifecycle

```
                    ┌──────────┐
                    │  REQUEST │
                    └────┬─────┘
                         ↓
              ┌──────────────────────┐
              │ Idempotency Check    │
              │ (order_id exists?)   │
              └────┬─────────────────┘
                   │ No duplicate
                   ↓
              ┌──────────────────────┐
              │ Redis DECR           │◄── Flash Sale Mode only
              │ (stock available?)   │
              └────┬─────────────────┘
                   │ Stock available
                   ↓
              ┌──────────────────────┐
              │ Atomic SQL Update    │
              │ (reserve in DB)      │
              └────┬─────────────────┘
                   │ rows affected = 1
                   ↓
              ┌──────────────────────┐
              │ Reservation Created  │
              │ Status: RESERVED     │
              └────┬─────────────────┘
                   │
       ┌───────────┼───────────┐
       ↓           ↓           ↓
  Payment OK   Payment Fail  Timeout
       ↓           ↓           ↓
  CONFIRMED    RELEASED     EXPIRED
```

---

## 🔁 Core Reservation Flow

**Happy path — successful reservation:**

```
User → POST /reservations
         ↓
    FlashSaleService
         ↓
    Redis DECR pre-check  (if flash sale enabled)
         ↓
    StockService — Atomic DB Update
         ↓
    ReservationService — Create record
         ↓
    Return: { status: RESERVED, expiresAt: ... }
```

**Confirm after payment:**

```
User → POST /reservations/{orderId}/confirm
         ↓
    RESERVED → CONFIRMED
    (stock deducted permanently)
```

**Release on failure or cancellation:**

```
User → POST /reservations/{orderId}/release
         ↓
    RESERVED → RELEASED
    (reserved_stock decremented, Redis compensated)
```

**Automatic expiry (no user action):**

```
Scheduler (every N seconds)
         ↓
    Scan WHERE status = RESERVED AND expires_at < NOW()
         ↓
    RESERVED → EXPIRED
    (reserved_stock released back)
```

---

## 📦 Module Structure

```
com.nazir.inventory
├── product        → Product management, stock tracking
├── reservation    → Reservation lifecycle (RESERVED / CONFIRMED / RELEASED / EXPIRED)
├── stock          → Atomic stock operations
├── flashsale      → Redis-backed flash sale mode
├── scheduler      → Expiry cleanup jobs
├── event          → Kafka event publishing
└── common         → Shared utilities, exceptions, constants
```

Each module owns its domain logic — no cross-module direct DB access.

---

## 🗄 Database Schema

### `product`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID / Long | Primary key |
| name | VARCHAR | Product name |
| total_stock | INT | Total available units |
| reserved_stock | INT | Currently held units |
| version | INT | Optimistic lock (future use) |
| created_at | TIMESTAMP | Record creation time |

> **Available stock** = `total_stock - reserved_stock`

### `reservation`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID / Long | Primary key |
| order_id | VARCHAR | **Unique** — idempotency key |
| product_id | FK | References `product.id` |
| quantity | INT | Units reserved |
| status | ENUM | RESERVED / CONFIRMED / RELEASED / EXPIRED |
| expires_at | TIMESTAMP | Reservation TTL |
| created_at | TIMESTAMP | Record creation time |

**Index:**

```sql
CREATE INDEX idx_reservation_status_expires
ON reservation (status, expires_at);
```

This index makes the expiry scheduler scan efficient at scale.

---

## 📊 APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/products` | Create a new product with stock |
| `GET` | `/api/v1/products/{id}` | Get product details and available stock |
| `POST` | `/api/v1/reservations` | Reserve stock for an order |
| `POST` | `/api/v1/reservations/{orderId}/confirm` | Confirm reservation after payment |
| `POST` | `/api/v1/reservations/{orderId}/release` | Release reservation on failure |

Full API docs available via Swagger UI after running locally.

---

## 🧪 Testing Strategy

### Unit Tests

| Scenario | Validates |
|----------|-----------|
| Successful reservation | Happy path, stock decremented correctly |
| Out-of-stock rejection | Returns error, no DB mutation |
| Duplicate order_id | Idempotency — second request rejected cleanly |

### Integration Tests

| Scenario | Validates |
|----------|-----------|
| Atomic DB update | Concurrent transactions do not oversell |
| Full reservation lifecycle | RESERVED → CONFIRMED / RELEASED / EXPIRED |
| Scheduler expiry | Expired reservations are cleaned up correctly |

### Concurrency Test

Simulates the core flash sale problem:

```
Threads : 100 concurrent requests
Stock   : 10 units

Expected results:
  ✅ Exactly 10 successful reservations
  ✅ 90 clean rejections
  ✅ reserved_stock never exceeds total_stock
  ✅ No deadlocks or exceptions
```

---

## ⚙️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| ORM | Spring Data JPA |
| Database | PostgreSQL |
| Cache / Guard | Redis |
| Messaging | Kafka |
| Containerization | Docker |
| Observability | Spring Actuator |
| API Docs | OpenAPI / Swagger |

---

## 🐳 Run Locally

**1. Start infrastructure:**

```bash
docker-compose up -d
```

This starts PostgreSQL, Redis, and Kafka containers.

**2. Build the project:**

```bash
mvn clean install
```

**3. Run the application:**

```bash
mvn spring-boot:run
```

**4. Open Swagger UI:**

```
http://localhost:8080/swagger-ui.html
```
