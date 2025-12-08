package com.kevdev.inventory.messaging.event;

import java.util.List;

public record InventoryReservationResultEvent(
        String orderId,
        String status,
        List<LineResult> lines
) {
    public record LineResult(
            String sku,
            String locationId,
            long requestedQuantity,
            long reservedQuantity,
            String status,
            String failureReason
    ) {
    }
}

