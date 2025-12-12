package com.kevdev.inventory.messaging.event;

public record OrderItemEvent(
        Long productId,
        String sku,
        String locationId,
        long quantity
) {
}


