package com.kevdev.inventory.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_sku_location",
                        columnNames = {"sku", "location_id"}
                )
        }
)
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(name = "location_id", nullable = false, length = 64)
    private String locationId;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryItem() {
        this.quantityOnHand = 0;
        this.quantityReserved = 0;
        this.updatedAt = Instant.now();
    }

    public InventoryItem(String sku,
                         String locationId,
                         Integer quantityOnHand,
                         Integer quantityReserved) {
        this.sku = sku;
        this.locationId = locationId;
        this.quantityOnHand = quantityOnHand != null ? quantityOnHand : 0;
        this.quantityReserved = quantityReserved != null ? quantityReserved : 0;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
        this.updatedAt = Instant.now();
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
        this.updatedAt = Instant.now();
    }

    // Main API used by InventoryServiceImpl

    public Integer getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(Integer quantityOnHand) {
        this.quantityOnHand = quantityOnHand != null ? quantityOnHand : 0;
        this.updatedAt = Instant.now();
    }

    public Integer getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(Integer quantityReserved) {
        this.quantityReserved = quantityReserved != null ? quantityReserved : 0;
        this.updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        int onHand = quantityOnHand != null ? quantityOnHand : 0;
        int reserved = quantityReserved != null ? quantityReserved : 0;
        return onHand - reserved;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    // Compatibility methods for ReservationServiceImpl

    public long getOnHandQuantity() {
        return quantityOnHand != null ? quantityOnHand.longValue() : 0L;
    }

    public long getReservedQuantity() {
        return quantityReserved != null ? quantityReserved.longValue() : 0L;
    }

    public void setOnHandQuantity(long onHandQuantity) {
        this.quantityOnHand = (int) onHandQuantity;
        this.updatedAt = Instant.now();
    }

    public void setReservedQuantity(long reservedQuantity) {
        this.quantityReserved = (int) reservedQuantity;
        this.updatedAt = Instant.now();
    }
}

