---
name: implement-phase
description: Implement đúng một phase theo banking-microservices-plan.md. Use when adding features by roadmap phase, continuing the plan, or checking phase checkpoints.
---

# Implement phase

## Workflow

1. Đọc `banking-microservices-plan.md` — xác định phase đang làm; chỉ implement đúng scope phase đó.
2. Tuân thủ package layout và conventions trong `AGENTS.md`.
3. Sau khi code: đảm bảo checkpoint của phase pass (build chạy, endpoint/demo được).
4. Không nhảy phase trước khi checkpoint hiện tại pass.

## Commands

```bash
./gradlew build
./gradlew :{service}:test
docker-compose up -d
./gradlew :{service}:bootRun
```

## Thứ tự phase

`0 → 1 → 2 → 3 → 4 → 5 → 6 → 7` — xem chi tiết và checkpoint trong `banking-microservices-plan.md`.
