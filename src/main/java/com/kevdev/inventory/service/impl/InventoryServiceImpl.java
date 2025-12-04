package com.kevdev.inventory.service.impl;

import com.kevdev.inventory.dto.StockAdjustmentRequestDto;
import com.kevdev.inventory.dto.StockResponseDto;
import com.kevdev.inventory.entity.InventoryItem;
import com.kevdev.inventory.entity.StockAdjustment;
import com.kevdev.inventory.repository.InventoryItemRepository;
import com.kevdev.inventory.repository.StockAdjustmentRepository;
import com.kevdev.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final String DEFAULT_LOCATION_ID = "MAIN";

    private final InventoryItemRepository inventoryItemRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;

    @Override
    public StockResponseDto getStock(String sku, String locationId) {
        String resolvedLocationId = resolveLocationId(locationId);
        InventoryItem item = inventoryItemRepository.findBySkuAndLocationId(sku, resolvedLocationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Inventory item not found for sku " + sku + " at location " + resolvedLocationId
                ));
        return mapToStockResponse(item);
    }

    @Override
    @Transactional
    public StockResponseDto adjustStock(StockAdjustmentRequestDto request) {
        String resolvedLocationId = resolveLocationId(request.locationId());

        if (request.quantityDelta() == null || request.quantityDelta() == 0L) {
            throw new IllegalArgumentException("quantityDelta must not be null or zero");
        }

        Instant now = Instant.now();

        InventoryItem item = inventoryItemRepository
                .findBySkuAndLocationId(request.sku(), resolvedLocationId)
                .orElseGet(() -> InventoryItem.builder()
                        .sku(request.sku())
                        .locationId(resolvedLocationId)
                        .onHandQuantity(0L)
                        .reservedQuantity(0L)
                        .safetyStock(null)
                        .reorderPoint(null)
                        .version(0L)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

        long newOnHand = item.getOnHandQuantity() + request.quantityDelta();
        if (newOnHand < 0) {
            throw new IllegalArgumentException("On hand quantity cannot be negative");
        }
        if (newOnHand < item.getReservedQuantity()) {
            throw new IllegalArgumentException("Adjustment would reduce on hand below reserved quantity");
        }

        item.setOnHandQuantity(newOnHand);
        if (item.getId() == null) {
            item.setCreatedAt(now);
        }
        item.setUpdatedAt(now);
        InventoryItem savedItem = inventoryItemRepository.save(item);

        StockAdjustment adjustment = StockAdjustment.builder()
                .inventoryItem(savedItem)
                .sku(savedItem.getSku())
                .locationId(savedItem.getLocationId())
                .quantityDelta(request.quantityDelta())
                .reason(request.reason())
                .createdAt(now)
                .createdBy(null)
                .build();

        stockAdjustmentRepository.save(adjustment);

        return mapToStockResponse(savedItem);
    }

    private String resolveLocationId(String locationId) {
        if (locationId == null || locationId.isBlank()) {
            return DEFAULT_LOCATION_ID;
        }
        return locationId;
    }

    private StockResponseDto mapToStockResponse(InventoryItem item) {
        long available = item.getOnHandQuantity() - item.getReservedQuantity();
        return new StockResponseDto(
                item.getSku(),
                item.getLocationId(),
                item.getOnHandQuantity(),
                item.getReservedQuantity(),
                available,
                item.getSafetyStock(),
                item.getReorderPoint()
        );
    }
}

