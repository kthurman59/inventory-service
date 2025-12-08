package com.kevdev.inventory.messaging.event;

import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        List<OrderItemEvent> items
) {
}
