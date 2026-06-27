<div align="center">

# 🏠 StayFinder

**A production-style property rental backend built to explore real-world engineering problems —
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

The booking hold uses a **two-layer concurrency guard**:

**Layer 1 — Redis distributed hold**
`SETNX hold:{propertyId}:{checkIn}:{checkOut}` is atomic. Only the first caller gets `OK`. Any concurrent request for the same key gets `0` and is rejected immediately. The key has a 30-second TTL, so stale holds expire automatically if the guest abandons the flow.

**Layer 2 — Optimistic locking on write**
Even if two requests somehow bypass Redis (e.g. hold expires mid-request), the `Booking` entity carries a `@Version` field. The second concurrent write hits an `OptimisticLockException` at the database transaction level, which rolls back cleanly.

```
Request A ──▶ SETNX hold key ──▶ OK  ──▶ INSERT booking ──▶ DEL hold ──▶ ✅ Confirmed
Request B ──▶ SETNX hold key ──▶ 0   ──▶ 409 Conflict                   ──▶ ❌ Rejected
```

---

## Technical Challenges

### 1. Cache Invalidation Across Three Stores

**Problem:** Property data lives in PostgreSQL, is cached in Redis, and is indexed in Elasticsearch. A property update (price change, new image) needs to be reflected in all three consistently. A naive approach leaves Redis serving stale data and Elasticsearch returning outdated documents in search results.

**Solution:** Writes to PostgreSQL are always followed by an explicit Redis cache eviction (`@CacheEvict`) and a synchronous Elasticsearch index update within the same service method. This is not a distributed transaction — if the ES update fails, the data is eventually corrected on the next read, which re-indexes the document. For the current scale this is acceptable; a production system would use an outbox pattern or CDC (Debezium).

---

### 2. Lazy Loading Breaking Redis Serialisation

**Problem:** When serialising a `Property` entity to JSON for the Redis cache, Jackson attempted to traverse the `imageUrls` collection. Because this was a Hibernate proxy backed by a closed `EntityManager` session, it threw a `LazyInitializationException` at cache write time — not at query time, making the stack trace misleading.

**Solution:** `@Transactional(readOnly = true)` on the service method keeps the session open through serialisation. The lazy collection is explicitly initialised into a plain `ArrayList` before the method returns, so the object stored in Redis has no Hibernate proxy references. Jackson is also configured with `activateDefaultTyping` so it stores the concrete class name alongside the JSON for correct deserialisation.

---

### 3. Elasticsearch Query API Migration

**Problem:** Elasticsearch's Java client replaced the old `RestHighLevelClient` with a new typed client between 7.x and 8.x. The old fluent chaining API (`QueryBuilders.range().field("pricePerNight").gte(min)`) does not compile against the 8.x client, which uses lambda builders exclusively.

**Solution:** All queries were rewritten to the lambda builder pattern:

```java
// Before (7.x — does not compile on 8.x)
Query.of(q -> q.range(r -> r.field("pricePerNight").gte(JsonData.of(min))))

// After (8.x lambda builder)
Query.of(q -> q.range(r -> r.number(n -> n.field("pricePerNight").gte(min.doubleValue()))))
```

The number-typed range builder is required for numeric fields; using the generic form produces a type mismatch at the Elasticsearch query DSL level, not at compile time — so it fails silently with 0 results.

---

### 4. Windows JVM Timezone Rejected by PostgreSQL 16

**Problem:** PostgreSQL 16 dropped support for deprecated IANA timezone aliases. The Windows JVM resolves `user.timezone` to `Asia/Calcutta`, which PostgreSQL 16 no longer accepts, causing the JDBC connection to fail at startup with `invalid value for parameter "TimeZone": "Asia/Calcutta"`.

**Solution:** Override the default timezone programmatically before the Spring context initialises:

```java
@SpringBootApplication
public class StayfinderApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(StayfinderApplication.class, args);
    }
}
```

This sets `Asia/Kolkata` (the current IANA canonical name) at JVM startup, before any JDBC connection is attempted.

---

## Tech Stack

| Technology | Role |
|------------|------|
| Java 21 | Language |
| Spring Boot 3.5 | Application framework |
| Spring Security + JJWT 0.12 | Stateless JWT auth, role-based access |
| Spring Data JPA + Hibernate 6 | ORM and database access |
| PostgreSQL 16 | Primary relational store |
| Redis 7 + Spring Data Redis | Response cache + distributed booking hold |
| Elasticsearch 8.13 | Full-text and filtered property search |
| Kibana 8.13 | Analytics dashboard over ES index |
| MinIO | S3-compatible self-hosted object storage for images |
| Spring Mail + Thymeleaf | Async HTML email notifications |
| Spring ApplicationEvent | Decoupled async event bus for booking lifecycle |
| Bucket4j 8.7 | Token bucket rate limiting per IP |
| SpringDoc OpenAPI 2.5 | Auto-generated Swagger UI |
| Docker + Docker Compose | Local infrastructure orchestration |
| Lombok | Boilerplate reduction |
| Maven | Build and dependency management |

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

## API Endpoints

| Method | Endpoint | Auth | Role |
|--------|----------|------|------|
| POST | `/api/users/register` | No | — |
| POST | `/api/auth/login` | No | — |
| GET | `/api/users/{id}` | Yes | Any |
| POST | `/api/properties` | Yes | HOST |
| GET | `/api/properties/{id}` | Yes | Any |
| PUT | `/api/properties/{id}` | Yes | HOST |
| DELETE | `/api/properties/{id}` | Yes | HOST |
| POST | `/api/properties/{id}/images` | Yes | HOST |
| POST | `/api/bookings/hold` | Yes | GUEST |
| POST | `/api/bookings` | Yes | GUEST |
| PATCH | `/api/bookings/{id}/confirm` | Yes | HOST |
| PATCH | `/api/bookings/{id}/cancel` | Yes | HOST / GUEST |
| GET | `/api/bookings/availability` | Yes | Any |
| GET | `/api/search` | Yes | Any |
| POST | `/api/reviews` | Yes | GUEST |
| GET | `/api/reviews/property/{id}/rating` | Yes | Any |
| GET | `/actuator/health` | No | — |

### Sample Requests

```bash
# Register
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Falak Khan","email":"falak@example.com","password":"secret123","role":"HOST"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"falak@example.com","password":"secret123"}'

# Search
curl -X GET "http://localhost:8080/api/search?city=Goa&minPrice=2000&maxPrice=8000&minGuests=2&keyword=beach" \
  -H "Authorization: Bearer <token>"

# Book
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"propertyId":"<uuid>","guestId":"<uuid>","checkIn":"2026-09-01","checkOut":"2026-09-05"}'

# Upload image
curl -X POST http://localhost:8080/api/properties/<id>/images \
  -H "Authorization: Bearer <token>" \
  -F "file=@/path/to/image.jpg"
```

---

## What I Would Do Differently

- **Outbox pattern** for cache + ES sync instead of in-process eviction — more reliable under partial failures
- **Kafka** instead of Spring Events for booking notifications — enables replay, fan-out, and independent consumer scaling
- **Geo-distance search** in Elasticsearch for "properties near me" queries
- **Idempotency keys** on booking creation to safely handle client retries
- **Kubernetes + HPA** for autoscaling the booking service under traffic spikes

---

## Authors

**Falak Khan** · [GitHub](https://github.com/falak-khan) · [LinkedIn](#)

**Fiza Khan** · [GitHub](https://github.com/fiza-khan) · [LinkedIn](#)

---

<div align="center">
<sub>Built with Spring Boot · PostgreSQL · Redis · Elasticsearch · MinIO</sub>
</div>
