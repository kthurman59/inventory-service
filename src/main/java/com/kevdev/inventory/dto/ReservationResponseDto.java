package com.kevdev.inventory.dto;

import java.util.List;

public record ReservationResponseDto(
        Long reservationId,
        String orderId,
        String status,
        String reason,
        List<ReservationItemResponseDto> items
) {
}

