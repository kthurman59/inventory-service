package com.kevdev.inventory.dto;

public class InventoryItemResponse {

    private String sku;
    private String locationId;
    private int quantityOnHand;
    private int quantityReserved;
    private int quantityAvailable;

    public InventoryItemResponse(String sku,
                                 String locationId,
                                 int quantityOnHand,
                                 int quantityReserved,
                                 int quantityAvailable) {
        this.sku = sku;
        this.locationId = locationId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.quantityAvailable = quantityAvailable;
    }

    public String getSku() {
        return sku;
    }

    public String getLocationId() {
        return locationId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }
}

