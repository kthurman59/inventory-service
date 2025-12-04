package com.kevdev.inventory.dto;

public record StockResponseDto(
        String sku,
        String locationId,
        long onHandQuantity,
        long reservedQuantity,
        long availableQuantity,
        Long safetyStock,
        Long reorderPoint
) {
}

