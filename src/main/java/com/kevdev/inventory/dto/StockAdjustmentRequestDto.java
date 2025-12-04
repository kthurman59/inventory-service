package com.kevdev.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequestDto(
        @NotBlank String sku,
        String locationId,
        @NotNull Long quantityDelta,
        String reason
) {
}

