---
name: flyway-migration
description: Tạo hoặc cập nhật Flyway migration khi entity/schema thay đổi. Use when changing JPA entities, adding columns/tables, or working on db/migration scripts.
---

# Flyway migration

## Rules

- Entity đổi schema → thêm script trong `src/main/resources/db/migration/`.
- Không dùng `ddl-auto: update` ngoài local dev.
- Database per Service — không migration/schema shared giữa services.

## Checklist

1. Viết migration `V{n}__{description}.sql` trong đúng service.
2. Entity/DTO/mapper khớp schema mới.
3. Chạy test service đó trước khi báo xong.
