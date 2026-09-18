CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    payment_id UUID,
    from_account_id UUID,
    to_account_id UUID,
    amount NUMERIC(19,2),
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_payment_id ON notifications (payment_id);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);

CREATE TABLE processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
