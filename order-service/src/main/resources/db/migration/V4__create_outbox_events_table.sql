CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               payload TEXT NOT NULL,
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               retry_count INTEGER DEFAULT 0

                               CONSTRAINT check_status CHECK (status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
