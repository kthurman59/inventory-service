package com.kevdev.inventory.controller;

import com.kevdev.inventory.dto.InventoryItemResponse;
import com.kevdev.inventory.dto.StockAdjustmentRequestDto;
import com.kevdev.inventory.dto.ReserveStockRequest;
import com.kevdev.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/items")
    public ResponseEntity<InventoryItemResponse> getInventory(
            @RequestParam String sku,
            @RequestParam String locationId
    ) {
        InventoryItemResponse response = inventoryService.getInventory(sku, locationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items/adjust")
    public ResponseEntity<InventoryItemResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequestDto request
    ) {
        InventoryItemResponse response = inventoryService.adjustStock(request);
        return ResponseEntity.ok(response);
    }

    // optional thin endpoints for simple reserve and release
    // paths changed so they do not overlap with ReservationController

    @PostMapping("/items/reserve")
    public ResponseEntity<Void> reserveStock(
            @Valid @RequestBody ReserveStockRequest request
    ) {
        inventoryService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/items/reserve/release")
    public ResponseEntity<Void> releaseStock(
            @Valid @RequestBody ReserveStockRequest request
    ) {
        inventoryService.releaseReserved(request);
        return ResponseEntity.noContent().build();
    }
}

