# Banking Microservices — Development Plan

## Mục tiêu
Portfolio project thể hiện năng lực: microservices architecture, saga pattern, event-driven design, security (JWT), containerization.

## Tech Stack
- **Java 25 + Spring Boot 4.1.x**
- **Spring Cloud**: Gateway, Eureka, Config Server
- **Kafka** (event bus)
- **PostgreSQL** (mỗi service 1 DB riêng — Database per Service)
- **Docker Compose**
- **JWT** (jjwt hoặc Spring Security OAuth2 Resource Server)
---

## Phase 0 — Infrastructure Setup
- [x] `docker-compose.yml`: Postgres (1 instance, 4 databases), Kafka KRaft, Kafka UI (debug)
- [x] **Eureka Server** — service registry
- [x] **Config Server** — centralized config, native file
- [x] **API Gateway** (Spring Cloud Gateway) — routing + JWT filter stub (validate ở Phase 1)

**Checkpoint:** Gateway route được tới 1 service dummy, đăng ký thành công trên Eureka. ✅

---

## Phase 1 — Auth Service
- [x] Entity: `User`, `Role`
- [x] Endpoint: `POST /register`, `POST /login`
- [x] JWT issue (access token + refresh token)
- [x] Password hashing (BCrypt)
- [x] Gateway: filter validate JWT, forward `X-User-Id` header xuống downstream services

**Checkpoint:** Login trả JWT, gọi API qua Gateway với token hợp lệ mới pass. ✅

---

## Phase 2 — Account Service
- [x] Entity: `Account` (id, userId, balance, currency, status)
- [x] Endpoint: `POST /accounts`, `GET /accounts/{id}`, `GET /accounts?userId=`
- [x] Internal API (không expose qua Gateway): `PUT /accounts/{id}/debit`, `PUT /accounts/{id}/credit`
  - Optimistic locking (`@Version`) để chống race condition khi 2 request đồng thời sửa balance
- [x] Kafka consumer: lắng nghe `DebitRequested`, `CreditRequested`

**Checkpoint:** Tạo account, debit/credit qua Kafka event, balance update đúng. ✅

---

## Phase 3 — Payment Service (Saga Orchestrator)
- [x] Entity: `Payment` (id, fromAccount, toAccount, amount, status: PENDING/COMPLETED/FAILED)
- [x] Endpoint: `POST /payments/transfer`
- [x] Saga flow (choreography, qua Kafka topics):
  1. Publish `DebitRequested`
  2. Consume `Debited` / `DebitFailed`
  3. Nếu Debited → publish `CreditRequested`
  4. Consume `Credited` / `CreditFailed`
  5. Nếu CreditFailed → publish `RefundRequested` (compensating action)
  6. Publish `TransferCompleted` / `TransferFailed`

**Checkpoint:** Transfer thành công end-to-end. Test case fail giữa chừng (VD: account B không tồn tại) → verify compensating transaction hoàn tiền A đúng. ✅

---

## Phase 4 — Transaction Service
- [x] Entity: `TransactionLog` (append-only, immutable)
- [x] Kafka consumer: subscribe tất cả events (Debited, Credited, TransferCompleted...) → ghi log
- [x] Endpoint: `GET /transactions?accountId=` (query history)

**Checkpoint:** Mọi transfer đều có log đầy đủ, query theo accountId trả đúng thứ tự thời gian. ✅

---

## Phase 5 — Notification Service
- [x] Kafka consumer: `TransferCompleted`, `TransferFailed`
- [x] Gửi email giả lập (log ra console hoặc dùng MailHog để test thật)

**Checkpoint:** Sau transfer, notification log/email xuất hiện. ✅

---

## Phase 6 — Cross-cutting Concerns
- [x] Centralized exception handling (`@ControllerAdvice` mỗi service)
- [x] Distributed tracing: **Zipkin/Sleuth** hoặc **Micrometer Tracing** — trace 1 request xuyên nhiều service
- [x] Resilience: **Resilience4j** — Circuit Breaker cho sync call (Gateway → Downstream services), Retry + DLT cho Kafka consumer
- [x] Idempotency: Kafka consumer phải idempotent (dùng eventId + dedup table) để tránh double-debit khi message bị redeliver

**Checkpoint:** Centralized error schema thống nhất, Zipkin distributed tracing & observation qua Kafka, Resilience4j Circuit Breaker fallback trên Gateway, Kafka consumer retry với BackOff/DLT và idempotency dedup table. ✅

---

## Phase 7 — Polish cho Portfolio
- [x] README rõ ràng: architecture diagram (Mermaid), sequence diagram, cách chạy `docker-compose up`, interview Q&A
- [x] Postman collection (`docs/postman_collection.json`) và Swagger/OpenAPI (`springdoc-openapi`) mỗi service
- [x] Unit test (service layer) + Integration test (Testcontainers cho Kafka + Postgres trên `payment-service`)
- [x] CI đơn giản (GitHub Actions: build + test trong `.github/workflows/ci.yml`)

**Checkpoint:** Hoàn thiện portfolio repo đầy đủ tài liệu, OpenAPI docs, Postman scripts, Testcontainers test, CI workflow, build và test thành công 100%. ✅

---

## Thứ tự thực hiện đề xuất
```
Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7
```
Mọi phases đều đã hoàn tất và vượt qua toàn bộ kiểm thử tự động.
