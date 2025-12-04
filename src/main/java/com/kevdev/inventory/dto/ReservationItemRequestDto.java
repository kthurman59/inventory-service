package com.kevdev.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservationItemRequestDto(
        @NotBlank String sku,
        String locationId,
        @NotNull @Min(1) Long quantity
) {
}

