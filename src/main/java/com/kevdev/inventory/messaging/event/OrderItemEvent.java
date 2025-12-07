package com.kevdev.inventory.messaging.event;

public record OrderItemEvent(
        Long productId,
        Integer quantity
) {}

