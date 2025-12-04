package com.kevdev.inventory.dto;

public record ReservationItemResponseDto(
        String sku,
        String locationId,
        long requestedQuantity,
        long reservedQuantity,
        String status,
        String failureReason
) {
}

