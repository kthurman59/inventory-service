package com.kevdev.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReservationCreateRequestDto(
        @NotBlank String orderId,
        @NotEmpty List<ReservationItemRequestDto> items
) {
}

