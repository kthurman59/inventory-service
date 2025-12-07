package com.kevdev.inventory.messaging.event;

public record InventoryReservationResultEvent(
        String orderId,
        String status,
        String reason
) {
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_FAILED = "FAILED";

    public static InventoryReservationResultEvent reserved(String orderId) {
        return new InventoryReservationResultEvent(orderId, STATUS_RESERVED, null);
    }

    public static InventoryReservationResultEvent failed(String orderId, String reason) {
        return new InventoryReservationResultEvent(orderId, STATUS_FAILED, reason);
    }
}

