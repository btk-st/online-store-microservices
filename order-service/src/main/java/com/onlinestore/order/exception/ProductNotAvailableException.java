package com.onlinestore.order.exception;

import java.util.UUID;

public class ProductNotAvailableException extends RuntimeException {
    private final UUID productId;
    private final int requestedQuantity;
    private final int availableQuantity;

    public ProductNotAvailableException(UUID productId, int requestedQuantity,
                                        int availableQuantity, String message) {
        super(String.format("Product %s not available. Requested: %d, Available: %d. %s",
                productId, requestedQuantity, availableQuantity, message));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    // геттеры
}
