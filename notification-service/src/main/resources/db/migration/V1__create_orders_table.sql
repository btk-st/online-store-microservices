CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID,
    quantity INTEGER,
    price DECIMAL(10, 2),
    sale DECIMAL(5, 2) DEFAULT 0.00,
    total_price DECIMAL(10, 2),
    user_id UUID NOT NULL
);
