package com.kevdev.inventory.controller;

import com.kevdev.inventory.dto.StockAdjustmentRequestDto;
import com.kevdev.inventory.dto.StockResponseDto;
import com.kevdev.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class StockController {

    private final InventoryService inventoryService;

    public StockController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stock")
    public StockResponseDto getStock(
            @RequestParam("sku") String sku,
            @RequestParam(value = "locationId", required = false) String locationId
    ) {
        return inventoryService.getStock(sku, locationId);
    }

    @PostMapping("/stock/adjust")
    public ResponseEntity<StockResponseDto> adjustStock(
            @Valid @RequestBody StockAdjustmentRequestDto request
    ) {
        StockResponseDto response = inventoryService.adjustStock(request);
        return ResponseEntity.ok(response);
    }
}

