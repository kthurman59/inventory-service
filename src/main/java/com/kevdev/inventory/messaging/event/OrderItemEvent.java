package com.kevdev.inventory.messaging.event;

public record OrderItemEvent(
        Long productId,
        String sku,
        Long quantity,
        String locationId
) {
}

