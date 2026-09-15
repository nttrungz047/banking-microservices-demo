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
- [ ] Entity: `User`, `Role`
- [ ] Endpoint: `POST /register`, `POST /login`
- [ ] JWT issue (access token + refresh token)
- [ ] Password hashing (BCrypt)
- [ ] Gateway: filter validate JWT, forward `X-User-Id` header xuống downstream services

**Checkpoint:** Login trả JWT, gọi API qua Gateway với token hợp lệ mới pass.

---

## Phase 2 — Account Service
- [ ] Entity: `Account` (id, userId, balance, currency, status)
- [ ] Endpoint: `POST /accounts`, `GET /accounts/{id}`, `GET /accounts?userId=`
- [ ] Internal API (không expose qua Gateway): `PUT /accounts/{id}/debit`, `PUT /accounts/{id}/credit`
  - Optimistic locking (`@Version`) để chống race condition khi 2 request đồng thời sửa balance
- [ ] Kafka consumer: lắng nghe `DebitRequested`, `CreditRequested`

**Checkpoint:** Tạo account, debit/credit qua Kafka event, balance update đúng.

---

## Phase 3 — Payment Service (Saga Orchestrator)
- [ ] Entity: `Payment` (id, fromAccount, toAccount, amount, status: PENDING/COMPLETED/FAILED)
- [ ] Endpoint: `POST /payments/transfer`
- [ ] Saga flow (choreography, qua Kafka topics):
  1. Publish `DebitRequested`
  2. Consume `Debited` / `DebitFailed`
  3. Nếu Debited → publish `CreditRequested`
  4. Consume `Credited` / `CreditFailed`
  5. Nếu CreditFailed → publish `RefundRequested` (compensating action)
  6. Publish `TransferCompleted` / `TransferFailed`

**Checkpoint:** Transfer thành công end-to-end. Test case fail giữa chừng (VD: account B không tồn tại) → verify compensating transaction hoàn tiền A đúng.

---

## Phase 4 — Transaction Service
- [ ] Entity: `TransactionLog` (append-only, immutable)
- [ ] Kafka consumer: subscribe tất cả events (Debited, Credited, TransferCompleted...) → ghi log
- [ ] Endpoint: `GET /transactions?accountId=` (query history)

**Checkpoint:** Mọi transfer đều có log đầy đủ, query theo accountId trả đúng thứ tự thời gian.

---

## Phase 5 — Notification Service
- [ ] Kafka consumer: `TransferCompleted`, `TransferFailed`
- [ ] Gửi email giả lập (log ra console hoặc dùng MailHog để test thật)

**Checkpoint:** Sau transfer, notification log/email xuất hiện.

---

## Phase 6 — Cross-cutting Concerns
- [ ] Centralized exception handling (`@ControllerAdvice` mỗi service)
- [ ] Distributed tracing: **Zipkin/Sleuth** hoặc **Micrometer Tracing** — trace 1 request xuyên nhiều service
- [ ] Resilience: **Resilience4j** — Circuit Breaker cho sync call (Gateway → Auth), Retry cho Kafka consumer
- [ ] Idempotency: Kafka consumer phải idempotent (dùng eventId + dedup table) để tránh double-debit khi message bị redeliver

---

## Phase 7 — Polish cho Portfolio
- [ ] README rõ ràng: architecture diagram, cách chạy `docker-compose up`
- [ ] Postman collection hoặc Swagger/OpenAPI mỗi service
- [ ] Unit test (service layer) + 1 Integration test (Testcontainers cho Kafka + Postgres)
- [ ] CI đơn giản (GitHub Actions: build + test)

---

## Thứ tự thực hiện đề xuất
```
Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7
```
Mỗi phase code xong nên demo chạy được (checkpoint) trước khi qua phase kế — tránh debug dồn cục cuối.

## Điểm sẽ được hỏi trong interview (map theo phase)
- Phase 0: Service discovery hoạt động thế nào? Gateway routing?
- Phase 1: JWT stateless vs session — trade-off? Refresh token flow?
- Phase 2: Optimistic vs Pessimistic locking cho balance update?
- Phase 3: Saga Choreography vs Orchestration? Sao không 2PC?
- Phase 4: Tại sao Transaction Service tách riêng khỏi Payment?
- Phase 6: Circuit Breaker states? Idempotency implement thế nào?
