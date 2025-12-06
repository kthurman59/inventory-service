package com.kevdev.inventory.dto;

import jakarta.validation.constraints.NotNull;

public class AdjustStockRequest {

    @NotNull
    private Integer quantityDelta;

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(Integer quantityDelta) {
        this.quantityDelta = quantityDelta;
    }
}

