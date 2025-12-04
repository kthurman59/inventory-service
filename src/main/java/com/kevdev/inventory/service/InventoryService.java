package com.kevdev.inventory.service;

import com.kevdev.inventory.dto.StockAdjustmentRequestDto;
import com.kevdev.inventory.dto.StockResponseDto;

public interface InventoryService {

    StockResponseDto getStock(String sku, String locationId);

    StockResponseDto adjustStock(StockAdjustmentRequestDto request);
}

