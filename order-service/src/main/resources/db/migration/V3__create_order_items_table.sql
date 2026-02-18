CREATE TABLE order_items (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id UUID NOT NULL,
                             product_name VARCHAR(255) NOT NULL,
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
                             sale DECIMAL(5, 2) DEFAULT 0 CHECK (sale >= 0 AND sale <= 100),
                             CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
