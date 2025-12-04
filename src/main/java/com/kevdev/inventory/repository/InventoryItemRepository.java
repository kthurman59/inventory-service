package com.kevdev.inventory.repository;

import com.kevdev.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findBySkuAndLocationId(String sku, String locationId);
}

