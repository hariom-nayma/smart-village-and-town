# Smart Village & Town Foundation (SVTF) — Production Backend

A high-performance, production-grade, multi-tenant Spring Boot backend designed for rural and municipal civic governance and local marketplace commerce.

---

## 🏛️ System Architecture Overview

The backend is built following **Clean / Hexagonal Domain-Driven Architecture** on **Java 21** and **Spring Boot 3.3.5**.

```
com.smartvillage
├── common/                    # Foundational cross-cutting concerns
│   ├── audit/                 # Asynchronous audit logging (audit_logs)
│   ├── config/                # OpenAPI 3 (Swagger), CORS, WebMvc, Async, JPA Auditing
│   ├── constant/              # System-wide roles and constants
│   ├── context/               # Multi-Tenant context holder (ThreadLocal + Filter)
│   ├── dto/                   # Standardized ApiResponse<T>, PagedResponse<T>, ErrorResponse
│   ├── entity/                # BaseEntity, TenantEntity
│   ├── exception/             # GlobalExceptionHandler, RFC 7807 compliant error models
│   ├── security/              # Stateless JWT Authentication, RBAC, UserPrincipal
│   ├── storage/               # FileStorageService (Local filesystem / S3 compatible)
│   └── util/                  # CodeGenerator (Order, Booking, OTP codes), SlugUtils
│
└── modules/                   # Autonomous Domain Modules
    ├── tenant/                # Multi-tenant village & town governance
    ├── auth/                  # JWT auth, Customer PIN login, user profiles
    ├── civic/                 # Complaints, Field Visits (V2.16.0), Volunteers, Initiatives
    ├── marketplace/           # Multi-vendor stores, catalog, cart, atomic checkout
    ├── delivery/              # Delivery fleet, assignments, 6-digit OTP verification, COD
    ├── payment/               # UPI payments, 12-digit UTR checks, screenshot verification
    ├── servicebooking/        # Local services booking, upfront fees, completion codes
    ├── transport/             # Passenger & cargo vehicles, per-km rates, trip booking
    └── notification/          # Targeted in-app campaigns & read receipts
```

---

## 🚀 Key Production Features

1. **Multi-Tenancy with Complete Isolation**:
   - Every transaction and entity is scoped to a specific `tenant_id`.
   - Dynamic resolution via `X-Tenant-Slug`, `X-Tenant-Id` headers or `?tenant=` query param.
   - Super Admin cross-tenant visibility.

2. **Constituency Service Operations (V2.16.0)**:
   - Nonpartisan grievance and case management for constituency and municipal offices.
   - Village and ward mapping, duplicate case linking, follow-up scheduling.
   - Staff-assigned field visit scheduling with purpose, outcome, and private internal notes.
   - Privacy-safe public case tracking via unique tracking IDs (`CMPT-YYYYMMDD-XXXX`).

3. **Multi-Vendor Atomic Checkout**:
   - Customer can place an order containing items from multiple vendors in a single checkout.
   - Transactional boundary automatically splits items into separate vendor orders.
   - Real-time stock decrement and vendor minimum order amount verification.
   - Automatic 5% upfront discount for UPI orders with instant UPI Intent URL generation (`upi://pay?pa=...`).

4. **Delivery Security & Fulfillment**:
   - Order assignment to verified delivery partners once vendor marks order `ready`.
   - Strict business guard: Unpaid UPI orders cannot be dispatched.
   - Out-for-delivery triggers 6-digit cryptographic random OTP, stored as SHA-256 hash in database.
   - Secure verification on doorstep: Delivery is completed only when customer provides matching OTP.
   - Cash on Delivery (COD) collection tracking.

5. **Local Services & Home Visit Bookings**:
   - Booking charge + visiting charge calculation.
   - 12-hour customer cancellation window policy with refund request tracking.
   - 6-digit completion code generated upon service finishing.
   - Vendor analytics (daily, weekly, monthly, yearly earnings & booking metrics).

6. **Transport, Passenger & Cargo Logistics**:
   - Vehicle registration with document verification (RC, driving licence, insurance, photo).
   - Distance-based per-km fare calculation with minimum fare safety floor.
   - Passenger vs cargo dimensions and weight capacity validation.

7. **Security & Role-Based Access Control**:
   - Stateless JWT tokens signed with HMAC-SHA256.
   - Roles: `SUPER_ADMIN`, `ADMIN`, `COMPLAINT_MANAGER`, `VOLUNTEER_MANAGER`, `VENDOR`, `DELIVERY_AGENT`, `CUSTOMER`.
   - One-time PIN generation for customer verification.

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| Runtime | Java 21 LTS |
| Framework | Spring Boot 3.3.5 |
| Persistence | Spring Data JPA (Hibernate 6.5) |
| Security | Spring Security 6 + JJWT 0.12.6 |
| Database | PostgreSQL 16 / H2 (Test) |
| Migrations | Flyway |
| Documentation | Springdoc OpenAPI 2.6.0 (Swagger UI) |
| Container | Docker & Docker Compose |

---

## 🏃 Running the Application

### Option A: Local Development with Maven

1. Start PostgreSQL (e.g. using `docker-compose up -d postgres`).
2. Run the Spring Boot application:
   ```bash
   mvn clean spring-boot:run
   ```
3. Open API documentation in browser:
   - **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Option B: Docker Compose (Full Stack)

```bash
docker-compose up --build -d
```
This starts both PostgreSQL and the Spring Boot application container with healthchecks and persistent volumes.

---

## 🧪 Testing

Run automated tests:
```bash
mvn clean test
```
The test suite utilizes in-memory PostgreSQL-mode H2 database with zero external dependencies.
# smart-village-and-town
