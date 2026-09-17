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
- **Dependency injection**: dùng constructor injection (`@RequiredArgsConstructor`); không dùng field injection.
- **Service contract**: business service luôn có interface và implementation; Controller chỉ phụ thuộc interface.
- **DTO & event**: request/response/Kafka event dùng immutable `record`; đặt tên rõ vai trò (`CreateXxxRequest`, `XxxResponse`, `XxxRequestedEvent`, `XxxCompletedEvent`, `XxxFailedEvent`). Không dùng raw `Map` hoặc `Object` làm event payload.
- **Naming**: package/class/method phải mô tả domain và hành vi; không dùng tên mơ hồ như `Data`, `Helper`, `Util`, `process` khi có tên nghiệp vụ cụ thể hơn.
- **Import & format**: dùng Google Java Format cho Java code; indent 4 spaces, không wildcard import, không unused import. Format code trước khi build/commit.
- **Mọi Kafka consumer phải idempotent**: check `eventId` đã xử lý chưa trước khi thực thi (dùng bảng `processed_events` hoặc tương đương).
- **Balance update dùng Optimistic Locking** (`@Version` trên entity `Account`) — không dùng `synchronized` hay lock tay.
- **Exception**: dùng custom exception (`AccountNotFoundException`, `InsufficientBalanceException`...) + `@RestControllerAdvice`, không throw generic `RuntimeException`.
- **Không hardcode config** (port, DB url, Kafka broker, consumer group, topic) trong Java code.
- **Naming Kafka topic**: `{domain}.{event-name}` viết thường, gạch nối — VD: `payment.debit-requested`, `payment.transfer-completed`.
- **Logging**: dùng `@Slf4j`, log ở mức Service layer khi có state change quan trọng (không log ở Controller).

## Centralized configuration

- Toàn bộ service business bắt buộc dùng Config Server; local `application.yml` chỉ giữ `spring.application.name` và import Config Server cần để bootstrap.
- Port, datasource, Kafka, Eureka, credentials và cấu hình theo profile phải đặt tại `config-server/src/main/resources/config/{service}.yml` hoặc shared `application.yml` của Config Server.
- Config Server là dependency bắt buộc: không cấu hình `optional:configserver` và không giữ local-dev fallback cho các cấu hình vận hành.
- Kafka bootstrap server, consumer group, topic name và retry/concurrency phải bind từ `@ConfigurationProperties` hoặc `${...}` properties; không dùng literal trong Java.

## Kafka & Saga rules

- Mỗi service dùng `@KafkaListener` phải khai báo `@EnableKafka`, `ConsumerFactory` và bean mặc định tên `kafkaListenerContainerFactory`; listener phải deserialize đúng payload contract.
- Kafka event là immutable, có `eventId`; event nằm trong Saga phải có `paymentId` hoặc correlation ID xuyên suốt toàn bộ flow.
- Thêm hoặc đổi event phải cập nhật producer, consumer, event schema, topic config và OpenAPI/README liên quan trong cùng thay đổi.
- Consumer chỉ ghi `processed_events` sau khi business action thành công hoặc đã publish failure outcome phù hợp; duplicate event phải an toàn và có log event ID.
- Saga phải có success path, failure path và compensating action. Không đánh dấu Saga terminal trước khi compensation trả kết quả (`refund-completed` hoặc `refund-failed`).
- Log state transition ở Service layer với event ID, aggregate/payment ID và trạng thái cũ/mới; không log dữ liệu nhạy cảm.
- Không xem `kafkaTemplate.send()` trong cùng DB transaction là bảo đảm delivery. Với flow cần độ tin cậy production, triển khai transactional outbox trước khi coi delivery là guaranteed.

## API & OpenAPI contract

- Mỗi service có REST API public bắt buộc có file `openapi-{service}.yaml` ở root của module.
- OpenAPI phải khớp source: path, HTTP method, authentication, request/response DTO, validation, HTTP status code và error response.
- Thay đổi public endpoint hoặc DTO là thay đổi contract: cập nhật OpenAPI trong cùng pull request.
- Không document internal endpoint như API Gateway public; endpoint nội bộ phải nêu rõ phạm vi và cơ chế auth service-to-service.

## Testing

- Unit test cho Service layer (Mockito) — bắt buộc cho mọi business logic mới.
- Integration test dùng **Testcontainers** (Postgres + Kafka thật, không mock).
- Không merge code thiếu test cho phần logic transfer/saga.
- Kafka/Saga test bắt buộc bao phủ: happy path, debit fail, credit fail + refund, refund fail và duplicate event/idempotency.
- Sau khi thêm listener, xác minh startup đã register consumer container và consumer group được tạo trên Kafka; không chỉ dừng ở compile.

## Khi agent tạo code mới

1. Đọc `banking-microservices-plan.md` để biết đang ở Phase nào, chỉ implement đúng scope của phase đó.
2. Tuân thủ cấu trúc thư mục ở trên, không tự ý đổi package layout.
3. Nếu thêm Kafka event mới → cập nhật cả producer và consumer trong cùng 1 lần thay đổi, không để lệch schema.
4. Nếu sửa entity có ảnh hưởng DB schema → tạo Flyway migration script trong `src/main/resources/db/migration/`, không dùng `ddl-auto: update` ở môi trường không phải local dev.
5. Nếu service có public REST API mới hoặc thay đổi API → tạo/cập nhật `openapi-{service}.yaml` cùng lần thay đổi.
6. Nếu thêm config service → tạo/cập nhật config tương ứng ở Config Server, không đặt local fallback.
7. Sau khi code xong 1 phase, đảm bảo checkpoint trong plan pass được: build/test chạy, endpoint gọi được, Kafka listener/group hoạt động nếu có event flow.

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
- Không merge public API thiếu OpenAPI contract hoặc Saga thiếu test success/failure/compensation.
