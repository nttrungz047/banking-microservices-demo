CREATE TABLE transaction_logs (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    payment_id UUID,
    account_id UUID,
    type VARCHAR(50) NOT NULL,
    amount NUMERIC(19,2),
    balance_after NUMERIC(19,2),
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transaction_logs_account_id ON transaction_logs (account_id, created_at);

CREATE TABLE processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
