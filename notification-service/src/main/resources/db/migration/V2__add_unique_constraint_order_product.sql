ALTER TABLE orders
ADD CONSTRAINT unique_order_product
UNIQUE (order_id, product_id);
