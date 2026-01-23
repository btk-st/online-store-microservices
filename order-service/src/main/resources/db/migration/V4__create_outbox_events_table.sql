-- V4__create_outbox_events_table.sql
CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               event_type VARCHAR(50) NOT NULL,
                               aggregate_type VARCHAR(50) NOT NULL,
                               aggregate_id VARCHAR(100) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               processed_at TIMESTAMP,
                               retry_count INTEGER DEFAULT 0,
                               error_message TEXT,

                               CONSTRAINT check_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
