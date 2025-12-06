package com.kevdev.inventory.service;

import com.kevdev.inventory.dto.InventoryItemResponse;
import com.kevdev.inventory.dto.StockAdjustmentRequestDto;
import com.kevdev.inventory.dto.ReserveStockRequest;

public interface InventoryService {

    InventoryItemResponse getInventory(String sku, String locationId);

    InventoryItemResponse adjustStock(StockAdjustmentRequestDto request);

    void reserveStock(ReserveStockRequest request);

    void releaseReserved(ReserveStockRequest request);
}

