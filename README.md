<div align="center">

# 🏠 StayFinder

**A property rental backend built to explore real-world engineering problems —
concurrent bookings, distributed caching, full-text search, and async event handling.**

![Java](https://img.shields.io/badge/Java-21-FF6B35?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.13-005571?style=flat-square&logo=elasticsearch&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

[Quick Start](#-setup) · [API Reference](#-api-endpoints) · [Architecture](#️-architecture) · [Challenges](#-technical-challenges)

</div>

---

## What Is StayFinder?

StayFinder is a **REST API backend** for a property rental platform. It covers the full lifecycle — property listing, availability search, concurrent booking management, image storage, and email notifications.

The focus is on solving a specific set of backend problems correctly:

- How do you prevent two guests from booking the same dates simultaneously?
- How do you search across thousands of properties without hitting the database on every request?
- How do you keep property data consistent across a relational database, a cache, and a search index at the same time?

---

## Core Features

| Feature | Description |
|---------|-------------|
| JWT Auth + RBAC | Stateless authentication. Roles: `HOST`, `GUEST`, `ADMIN` |
| Property CRUD | Full lifecycle management with image upload to MinIO |
| Booking Engine | Conflict detection, state machine (`PENDING → CONFIRMED → CANCELLED`) |
| Concurrent Hold | Redis-based 30-second distributed hold to prevent double booking |
| Full-text Search | Elasticsearch with city, price range, guest count, and keyword filters |
| Response Caching | Redis cache-aside for property reads; invalidated on write |
| Review System | Post-stay ratings with automatic average recalculation synced to ES index |
| Email Notifications | Async HTML emails via Spring Events + Thymeleaf on booking lifecycle changes |
| Analytics | Kibana dashboard over Elasticsearch index — property distribution by city, price |
| Rate Limiting | Bucket4j token bucket — 100 requests/minute per IP |
| API Docs | Auto-generated Swagger UI via SpringDoc OpenAPI |

---

## Architecture

### System Overview

```
┌────────────────────────────────────────────────────────────┐
│                     Client (HTTP)                          │
└────────────────────────┬───────────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────────┐
│               Spring Boot Application                      │
│                                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐  │
│  │ JWT Filter   │→ │ Controllers  │→ │    Services      │  │
│  │ Rate Limiter │  │ (REST Layer) │  │ (Business Logic) │  │
│  └──────────────┘  └──────────────┘  └────────┬────────┘  │
│                                               │            │
│              ┌────────────────────────────────┤            │
│              │            │           │       │            │
│              ▼            ▼           ▼       ▼            │
│         ┌────────┐  ┌─────────┐ ┌───────┐ ┌──────┐        │
│         │Postgres│  │  Redis  │ │  ES   │ │MinIO │        │
│         │  (DB)  │  │(Cache + │ │Search │ │Files │        │
│         │        │  │  Hold)  │ │ Index │ │      │        │
│         └────────┘  └─────────┘ └───────┘ └──────┘        │
│                                                            │
│         ┌──────────────────────────────────────────┐       │
│         │  Spring Event Bus (Async)                │       │
│         │  BookingCreated / BookingConfirmed Events│       │
│         └──────────────────┬───────────────────────┘       │
│                            ▼                               │
│                    ┌──────────────┐                        │
│                    │ Email Service│                        │
│                    │(SMTP/Gmail)  │                        │
│                    └──────────────┘                        │
└────────────────────────────────────────────────────────────┘
```

### Booking Flow — Sequence

```
Guest                  API                  Redis              PostgreSQL
  │                     │                     │                     │
  │── POST /hold ───────▶                     │                     │
  │                     │── SETNX hold key ──▶│                     │
  │                     │◀─ OK (30s TTL) ─────│                     │
  │◀── 200 Hold placed ─│                     │                     │
  │                     │                     │                     │
  │── POST /bookings ───▶                     │                     │
  │                     │── GET hold key ─────▶                     │
  │                     │◀─ exists ───────────│                     │
  │                     │── overlap query ─────────────────────────▶│
  │                     │◀─ no conflict ───────────────────────────-│
  │                     │── INSERT booking ────────────────────────▶│
  │                     │── DEL hold key ─────▶                     │
  │◀── 201 Created ─────│                     │                     │
  │                     │                     │                     │
  │         [async]     │                     │                     │
  │◀── Booking email ───│                     │                     │
```

---

## Class Diagram

```
┌─────────────────┐         ┌──────────────────┐
│      User       │         │     Property     │
├─────────────────┤         ├──────────────────┤
│ id: UUID        │1      * │ id: UUID         │
│ name: String    ├─────────│ title: String    │
│ email: String   │  owns   │ city: String     │
│ password: String│         │ pricePerNight:   │
│ role: Role      │         │   BigDecimal     │
└────────┬────────┘         │ maxGuests: int   │
         │                  │ imageUrls: List  │
         │ books            │ owner: User      │
         │ *                └───────┬──────────┘
┌────────▼────────┐                │ 1
│     Booking     │                │
├─────────────────┤                │ has many
│ id: UUID        │*               ▼
│ guest: User     │         ┌──────────────────┐
│ property:       ├─────────│     Review       │
│   Property      │  for    ├──────────────────┤
│ checkIn: Date   │         │ id: UUID         │
│ checkOut: Date  │         │ rating: int      │
│ totalPrice:     │         │ comment: String  │
│   BigDecimal    │         │ guest: User      │
│ status:         │         │ property:        │
│  BookingStatus  │         │   Property       │
└────────┬────────┘         └──────────────────┘
         │
         │ tracks
         ▼
┌─────────────────────┐       ┌──────────────────┐
│ BookingStatusHistory│       │  BookingStatus   │
├─────────────────────┤       ├──────────────────┤
│ id: UUID            │       │  PENDING         │
│ booking: Booking    │       │  CONFIRMED       │
│ status:             │       │  CANCELLED       │
│  BookingStatus      │       └──────────────────┘
│ changedAt: Instant  │
└─────────────────────┘

┌──────────────┐
│     Role     │
├──────────────┤
│  HOST        │
│  GUEST       │
│  ADMIN       │
└──────────────┘
```

---

## Preventing Double Booking

Two-layer concurrency guard — neither layer alone is sufficient.

**Layer 1 — Redis distributed hold (`SETNX`)**
- Guest calls `POST /bookings/hold` before confirming a booking.
- `SETNX hold:{propertyId}:{checkIn}:{checkOut}` is atomic — only one caller gets `OK`.
- Any concurrent request for the same key gets `0` and receives a `409 Conflict` immediately.
- Key has a 30-second TTL — stale holds expire automatically if the guest abandons the flow.

**Layer 2 — Optimistic locking on write (`@Version`)**
- Even if two requests bypass Redis (e.g. hold expires mid-request), the `Booking` entity carries a `@Version` field.
- The second concurrent write throws `OptimisticLockException` at the database transaction level and rolls back cleanly.
- No explicit database lock is held, so read throughput is not affected.

```
Request A ──▶ SETNX ──▶ OK  ──▶ overlap check ──▶ INSERT ──▶ DEL hold ──▶ ✅ Confirmed
Request B ──▶ SETNX ──▶ 0   ──▶ 409 Conflict                             ──▶ ❌ Rejected
```

---

## Technical Challenges

### 1. Cache Invalidation Across Three Stores

- **Problem:** Property data lives in PostgreSQL, is cached in Redis, and is indexed in Elasticsearch. A single property update — price change, new image — has to be reflected consistently in all three. A partial update leaves Redis serving stale data while Elasticsearch returns outdated documents in search results.
- **Solution:** Every write to PostgreSQL is followed by an explicit `@CacheEvict` on the Redis key and a synchronous ES index update within the same service method.
- **Trade-off:** This is not a distributed transaction. If the ES update fails after the DB write succeeds, data is eventually corrected on the next read by re-indexing the document. Acceptable at this scale; a production system would use an outbox pattern or CDC via Debezium.

---

### 2. Hibernate Lazy Proxy Breaking Redis Serialisation

- **Problem:** Caching a `Property` entity in Redis triggers Jackson serialisation. Jackson attempts to traverse the `imageUrls` collection, which is a Hibernate proxy — but by that point the `EntityManager` session is already closed. This throws `LazyInitializationException` at cache-write time, not at query time, making the stack trace misleading.
- **Solution:**
  - Added `@Transactional(readOnly = true)` on the service read method to keep the session open through serialisation.
  - Explicitly copied the lazy collection into a plain `ArrayList` before the method returns — no Hibernate proxy references survive into the cached object.
  - Configured Jackson with `activateDefaultTyping` so Redis stores the concrete class name alongside the JSON, enabling correct deserialisation on cache hit.

---

### 3. Elasticsearch 8.x Query API — Silent Result Failures

- **Problem:** Elasticsearch replaced `RestHighLevelClient` with a new strongly-typed Java client in 8.x. The old fluent chaining API does not compile. Worse — using the generic range builder on a numeric field does not throw a compile-time or runtime error; it just returns 0 results silently, making it very hard to diagnose.
- **Root cause:** The generic range builder sends an untyped DSL fragment that Elasticsearch accepts syntactically but does not match any numeric mapping at query time.
- **Solution:** Migrated all queries to the lambda builder pattern and switched to the number-typed range explicitly:

```java
// Generic form — silently returns 0 results on numeric fields
Query.of(q -> q.range(r -> r.field("pricePerNight").gte(JsonData.of(min))))

// Correct — number-typed range builder
Query.of(q -> q.range(r -> r.number(n -> n.field("pricePerNight").gte(min.doubleValue()))))
```

---

### 4. Windows JVM Timezone Alias Rejected by PostgreSQL 16

- **Problem:** PostgreSQL 16 dropped support for deprecated IANA timezone aliases. On Windows, the JVM resolves `user.timezone` to `Asia/Calcutta` — a deprecated alias — causing the JDBC connection pool to fail at startup with `invalid value for parameter "TimeZone": "Asia/Calcutta"`. The error appears before the Spring context loads, so it looks like an infrastructure failure rather than a configuration issue.
- **Solution:** Override the JVM default timezone before the Spring context initialises:

```java
@SpringBootApplication
public class StayfinderApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(StayfinderApplication.class, args);
    }
}
```

- **Why this works:** `Asia/Kolkata` is the current canonical IANA name. Setting it before `SpringApplication.run` ensures JDBC picks it up before opening any connections.

---

## Tech Stack

### Backend

| Technology | Role |
|------------|------|
| Java 21 | Language |
| Spring Boot 3.5 | Application framework |
| Spring Security + JJWT 0.12 | Stateless JWT authentication, role-based access control |
| Spring Data JPA + Hibernate 6 | ORM and relational database access |
| Spring Mail + Thymeleaf | Async HTML email notifications |
| Spring ApplicationEvent | Decoupled async event bus for booking lifecycle |
| Bucket4j 8.7 | Token bucket rate limiting — 100 req/min per IP |
| SpringDoc OpenAPI 2.5 | Auto-generated Swagger UI |
| Lombok | Boilerplate reduction (`@Builder`, `@Getter`, etc.) |
| Maven | Build and dependency management |

### Database & Storage

| Technology | Role |
|------------|------|
| PostgreSQL 16 | Primary relational store — users, bookings, reviews |
| Redis 7 | Response cache (cache-aside) + distributed booking hold (`SETNX`) |
| Elasticsearch 8.13 | Full-text and filtered property search index |
| MinIO | S3-compatible self-hosted object storage for property images |

### Analytics & Observability

| Technology | Role |
|------------|------|
| Kibana 8.13 | Analytics dashboard over Elasticsearch index |
| Spring Actuator | Health check endpoint (`/actuator/health`) |

### Infrastructure

| Technology | Role |
|------------|------|
| Docker + Docker Compose | Local orchestration — single command starts all services |

> **Frontend:** This is a backend-only project. The API is consumed via Swagger UI (`/swagger-ui/index.html`) or any REST client. A frontend can be built on top of the existing endpoints.

---

## Project Structure

```
src/main/java/com/example/stayfinder/
├── config/
│   ├── MinioConfig.java
│   ├── RedisConfig.java
│   ├── RateLimitFilter.java
│   └── SwaggerConfig.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── PropertyController.java
│   ├── BookingController.java
│   ├── SearchController.java
│   └── ReviewController.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── PropertyService.java
│   ├── BookingService.java
│   ├── BookingHoldService.java
│   ├── PropertySearchService.java
│   ├── ReviewService.java
│   ├── EmailService.java
│   └── MinioService.java
├── entity/
│   ├── User.java
│   ├── Property.java
│   ├── Booking.java
│   ├── BookingStatusHistory.java
│   ├── Review.java
│   ├── Role.java (enum)
│   └── BookingStatus.java (enum)
├── dto/
├── repository/
├── document/
│   └── PropertyDocument.java       ← ES index mapping
├── security/
│   ├── JwtAuthFilter.java
│   └── SecurityConfig.java
├── event/
│   ├── BookingCreatedEvent.java
│   ├── BookingConfirmedEvent.java
│   └── BookingEventListener.java
└── util/
    └── JwtUtil.java

src/main/resources/
├── application.properties
└── templates/
    ├── booking-created.html
    └── booking-confirmed.html
```

---

## Setup

### Prerequisites

- Java 21
- Maven 3.8+
- Docker Desktop (running)

### Run

```bash
git clone https://github.com/falak-khan/stayfinder.git
cd stayfinder

# Start all infrastructure
docker-compose up -d

# Verify containers
docker-compose ps   # expect: postgres, redis, elasticsearch, kibana, minio — all Up
```

**MinIO bucket setup** (one-time):
1. Open `http://localhost:9001` → login `minioadmin / minioadmin`
2. Create bucket named `stayfinder`, set access policy to **Public**

**application.properties** — fill in before running:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/stayfinder
spring.datasource.username=postgres
spring.datasource.password=postgres

minio.url=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket=stayfinder

spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.elasticsearch.uris=http://localhost:9200

jwt.secret=your-secret-key-minimum-32-characters
jwt.expiration=86400000

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_GMAIL
spring.mail.password=YOUR_APP_PASSWORD    # Gmail → Security → App Passwords
```

```bash
./mvnw spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`
Kibana: `http://localhost:5601`

---


## Authors

**Falak Khan** · [GitHub](https://github.com/falak-khan) · [LinkedIn](#)

**Fiza Khan** · [GitHub](https://github.com/fiza-khan) · [LinkedIn](#)

---

<div align="center">
<sub>Built with Spring Boot · PostgreSQL · Redis · Elasticsearch · MinIO</sub>
</div>
