package com.kevdev.inventory.service.impl;

import com.kevdev.inventory.dto.InventoryItemResponse;
import com.kevdev.inventory.dto.ReserveStockRequest;
import com.kevdev.inventory.dto.StockAdjustmentRequestDto;
import com.kevdev.inventory.entity.InventoryItem;
import com.kevdev.inventory.repository.InventoryItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    InventoryItemRepository inventoryItemRepository;

    private InventoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InventoryServiceImpl(inventoryItemRepository);
    }

    @Test
    void getInventory_whenFound_returnsResponse() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 2);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        InventoryItemResponse resp = service.getInventory("SKU1", "MAIN");

        assertThat(resp).isNotNull();
        assertThat(resp.getSku()).isEqualTo("SKU1");
        assertThat(resp.getLocationId()).isEqualTo("MAIN");
        assertThat(resp.getQuantityOnHand()).isEqualTo(10);
        assertThat(resp.getQuantityReserved()).isEqualTo(2);
        assertThat(resp.getQuantityAvailable())
        .isEqualTo(item.getAvailableQuantity());

    }

    @Test
    void getInventory_whenMissing_throwsEntityNotFound() {
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getInventory("SKU1", "MAIN"));
    }

    @Test
    void adjustStock_whenExisting_updatesOnHand_andSaves() {
        InventoryItem existing = new InventoryItem("SKU1", "MAIN", 10, 1);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(existing));

        StockAdjustmentRequestDto req = stockAdj("SKU1", "MAIN", 5);

        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItemResponse resp = service.adjustStock(req);

        assertThat(resp.getQuantityOnHand()).isEqualTo(15);
        verify(inventoryItemRepository).save(existing);
    }

    @Test
    void adjustStock_whenMissing_createsNewRow_andSaves() {
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.empty());

        StockAdjustmentRequestDto req = stockAdj("SKU1", "MAIN", 7);

        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItemResponse resp = service.adjustStock(req);

        assertThat(resp.getSku()).isEqualTo("SKU1");
        assertThat(resp.getLocationId()).isEqualTo("MAIN");
        assertThat(resp.getQuantityOnHand()).isEqualTo(7);
        assertThat(resp.getQuantityReserved()).isEqualTo(0);
        verify(inventoryItemRepository).save(any(InventoryItem.class));
    }

    @Test
    void adjustStock_whenWouldGoNegative_throws_andDoesNotSave() {
        InventoryItem existing = new InventoryItem("SKU1", "MAIN", 3, 0);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(existing));

        StockAdjustmentRequestDto req = stockAdj("SKU1", "MAIN", -10);

        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(req));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void reserveStock_whenEnoughAvailable_increasesReserved_andSaves() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 2);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        ReserveStockRequest req = reserveReq("SKU1", "MAIN", 5);

        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.reserveStock(req);

        assertThat(item.getQuantityReserved()).isEqualTo(7);
        verify(inventoryItemRepository).save(item);
    }

    @Test
    void reserveStock_whenInsufficient_throws_andDoesNotSave() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 9);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        ReserveStockRequest req = reserveReq("SKU1", "MAIN", 5);

        assertThrows(IllegalArgumentException.class, () -> service.reserveStock(req));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void reserveStock_whenMissing_throwsEntityNotFound() {
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.empty());

        ReserveStockRequest req = reserveReq("SKU1", "MAIN", 1);

        assertThrows(EntityNotFoundException.class, () -> service.reserveStock(req));
    }

    @Test
    void releaseReserved_whenValid_decreasesReserved_andSaves() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 5);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        ReserveStockRequest req = reserveReq("SKU1", "MAIN", 3);

        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.releaseReserved(req);

        assertThat(item.getQuantityReserved()).isEqualTo(2);
        verify(inventoryItemRepository).save(item);
    }

    @Test
    void releaseReserved_whenWouldGoNegative_throws_andDoesNotSave() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 2);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        ReserveStockRequest req = reserveReq("SKU1", "MAIN", 5);

        assertThrows(IllegalArgumentException.class, () -> service.releaseReserved(req));
        verify(inventoryItemRepository, never()).save(any());
    }

    @Test
    void releaseReserved_whenMissing_throwsEntityNotFound() {
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.empty());

        ReserveStockRequest req = reserveReq("SKU1", "MAIN", 1);

        assertThrows(EntityNotFoundException.class, () -> service.releaseReserved(req));
    }

    private static StockAdjustmentRequestDto stockAdj(String sku, String locationId, int delta) {
        StockAdjustmentRequestDto req = new StockAdjustmentRequestDto();
        req.setSku(sku);
        req.setLocationId(locationId);
        req.setQuantityDelta(delta);
        return req;
    }

    private static ReserveStockRequest reserveReq(String sku, String locationId, int qty) {
        ReserveStockRequest req = new ReserveStockRequest();
        req.setSku(sku);
        req.setLocationId(locationId);
        req.setQuantity(qty);
        return req;
    }
}

