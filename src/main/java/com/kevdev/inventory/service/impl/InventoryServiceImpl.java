package com.kevdev.inventory.service.impl;

import com.kevdev.inventory.dto.InventoryItemResponse;
import com.kevdev.inventory.dto.StockAdjustmentRequestDto;
import com.kevdev.inventory.dto.ReserveStockRequest;
import com.kevdev.inventory.entity.InventoryItem;
import com.kevdev.inventory.repository.InventoryItemRepository;
import com.kevdev.inventory.service.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryServiceImpl(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResponse getInventory(@NotBlank String sku, @NotBlank String locationId) {
        InventoryItem item = inventoryItemRepository.findBySkuAndLocationId(sku, locationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inventory not found for sku " + sku + " and location " + locationId));

        return toResponse(item);
    }

    @Override
    @Transactional
    public InventoryItemResponse adjustStock(StockAdjustmentRequestDto request) {
        InventoryItem item = inventoryItemRepository.findBySkuAndLocationId(
                        request.getSku(), request.getLocationId())
                .orElseGet(() -> new InventoryItem(
                        request.getSku(),
                        request.getLocationId(),
                        0,
                        0
                ));

        int newOnHand = item.getQuantityOnHand() + request.getQuantityDelta();
        if (newOnHand < 0) {
            throw new IllegalArgumentException("Resulting quantity on hand would be negative");
        }

        item.setQuantityOnHand(newOnHand);
        InventoryItem saved = inventoryItemRepository.save(item);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void reserveStock(ReserveStockRequest request) {
        InventoryItem item = inventoryItemRepository.findBySkuAndLocationId(
                        request.getSku(), request.getLocationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inventory not found for sku " + request.getSku() +
                                " and location " + request.getLocationId()));

        int available = item.getAvailableQuantity();
        if (available < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock available for sku " +
                    request.getSku() + " at location " + request.getLocationId());
        }

        item.setQuantityReserved(item.getQuantityReserved() + request.getQuantity());
        inventoryItemRepository.save(item);
    }

    @Override
    @Transactional
    public void releaseReserved(ReserveStockRequest request) {
        InventoryItem item = inventoryItemRepository.findBySkuAndLocationId(
                        request.getSku(), request.getLocationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Inventory not found for sku " + request.getSku() +
                                " and location " + request.getLocationId()));

        int newReserved = item.getQuantityReserved() - request.getQuantity();
        if (newReserved < 0) {
            throw new IllegalArgumentException("Cannot release more reserved than currently reserved");
        }

        item.setQuantityReserved(newReserved);
        inventoryItemRepository.save(item);
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getSku(),
                item.getLocationId(),
                item.getQuantityOnHand(),
                item.getQuantityReserved(),
                item.getAvailableQuantity()
        );
    }
}

