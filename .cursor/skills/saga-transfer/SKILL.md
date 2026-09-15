---
name: saga-transfer
description: Implement hoặc sửa money transfer bằng Choreography Saga qua Kafka. Use when working on payment transfer, debit/credit flow, compensating transactions, or saga orchestration.
---

# Saga transfer (choreography)

## Flow

```
Payment: publish DebitRequested
  → Account: debit A → Debited | DebitFailed
Payment: Debited → publish CreditRequested
  → Account: credit B → Credited | CreditFailed
    → CreditFailed: publish RefundRequested (hoàn tiền A)
Payment: publish TransferCompleted | TransferFailed
Transaction: consume events → log
Notification: TransferCompleted → notify
```

## Hard rules

- Không dùng 2PC / distributed transaction — dùng Saga.
- Không expose debit/credit qua Gateway.
- Consumer idempotent (`eventId` + dedup).
- Balance: Optimistic Locking `@Version`.
- Test fail giữa chừng (VD: account B không tồn tại) → verify refund A đúng.
