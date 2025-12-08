package com.kevdev.inventory.service;

import com.kevdev.inventory.dto.ReservationCreateRequestDto;
import com.kevdev.inventory.dto.ReservationResponseDto;
import com.kevdev.inventory.messaging.event.OrderCreatedEvent;

public interface ReservationService {

    ReservationResponseDto createReservation(ReservationCreateRequestDto request);

    ReservationResponseDto commitReservation(Long reservationId, String reason);

    ReservationResponseDto releaseReservation(Long reservationId, String reason);

    ReservationResponseDto getReservationByOrderId(String orderId);

    /**
     * Kafka entry point, called when an OrderCreatedEvent arrives.
     */
    ReservationResponseDto reserveForOrder(OrderCreatedEvent event);
}

