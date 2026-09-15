# AGENTS.md

Hướng dẫn cho AI coding agent (Claude Code, v.v.) khi làm việc trong repo `banking-microservices`.

## Project context

Portfolio project: hệ thống banking microservices. Chi tiết kiến trúc xem `README.md`, roadmap xem `banking-microservices-plan.md`.

## Tech stack — bắt buộc tuân thủ

- Java 25
- Spring Boot 4.1.x
- Gradle 9.x (multi-module)

- Spring Cloud 2025.1.x
    - Gateway
    - Eureka Client
    - Config Client
    - OpenFeign

- Apache Kafka
    - Async communication / event-driven communication
- REST/OpenFeign
    - Sync request/response khi thực sự cần

- PostgreSQL
    - Database per service
    - Không share DB giữa các service

- Lombok
    - @Getter/@Setter
    - @Builder
    - @Slf4j

- MapStruct
    - DTO ↔ Entity mapping
    - Không map thủ công

## Cấu trúc mỗi service (bắt buộc theo layered architecture)

```
{service}/src/main/java/com/banking/{service}/
├── controller/      # REST endpoint, không chứa business logic
├── service/         # business logic, interface + impl
├── repository/       # Spring Data JPA
├── entity/          # JPA entity
├── dto/             # request/response DTO, không expose entity ra ngoài
├── mapper/           # MapStruct
├── event/            # Kafka event DTO (producer + consumer)
├── config/           # Kafka, Security config
└── exception/        # custom exception + GlobalExceptionHandler
```

## Coding conventions

- **Controller không gọi Repository trực tiếp** — luôn qua Service layer.
- **Entity không được trả về qua API** — luôn map sang DTO.
- **Mọi Kafka consumer phải idempotent**: check `eventId` đã xử lý chưa trước khi thực thi (dùng bảng `processed_events` hoặc tương đương).
- **Balance update dùng Optimistic Locking** (`@Version` trên entity `Account`) — không dùng `synchronized` hay lock tay.
- **Exception**: dùng custom exception (`AccountNotFoundException`, `InsufficientBalanceException`...) + `@RestControllerAdvice`, không throw generic `RuntimeException`.
- **Không hardcode config** (port, DB url, Kafka broker) — lấy từ Config Server / `application.yml` theo profile (`dev`, `docker`).
- **Naming Kafka topic**: `{domain}.{event-name}` viết thường, gạch nối — VD: `payment.debit-requested`, `payment.transfer-completed`.
- **Logging**: dùng `@Slf4j`, log ở mức Service layer khi có state change quan trọng (không log ở Controller).

## Testing

- Unit test cho Service layer (Mockito) — bắt buộc cho mọi business logic mới.
- Integration test dùng **Testcontainers** (Postgres + Kafka thật, không mock).
- Không merge code thiếu test cho phần logic transfer/saga.

## Khi agent tạo code mới

1. Đọc `banking-microservices-plan.md` để biết đang ở Phase nào, chỉ implement đúng scope của phase đó.
2. Tuân thủ cấu trúc thư mục ở trên, không tự ý đổi package layout.
3. Nếu thêm Kafka event mới → cập nhật cả producer và consumer trong cùng 1 lần thay đổi, không để lệch schema.
4. Nếu sửa entity có ảnh hưởng DB schema → tạo Flyway migration script trong `src/main/resources/db/migration/`, không dùng `ddl-auto: update` ở môi trường không phải local dev.
5. Sau khi code xong 1 phase, đảm bảo checkpoint trong plan pass được (build chạy, endpoint gọi được) trước khi báo hoàn thành.

## Build & run commands

```bash
./gradlew build                    # build toàn bộ multi-module
./gradlew :account-service:test    # test 1 service cụ thể
docker-compose up -d                # infra (Postgres, Kafka)
./gradlew :account-service:bootRun # chạy 1 service
```

## Không được làm

- Không gọi trực tiếp DB của service khác (vi phạm Database per Service).
- Không dùng 2PC / distributed transaction cho transfer — dùng Saga.
- Không expose internal endpoint (debit/credit) ra ngoài qua Gateway — chỉ nội bộ qua Kafka hoặc service-to-service có auth riêng.
