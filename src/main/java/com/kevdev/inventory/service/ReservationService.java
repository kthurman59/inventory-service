package com.kevdev.inventory.service;

import com.kevdev.inventory.dto.ReservationCreateRequestDto;
import com.kevdev.inventory.dto.ReservationResponseDto;

public interface ReservationService {

    ReservationResponseDto createReservation(ReservationCreateRequestDto request);

    ReservationResponseDto commitReservation(Long reservationId, String reason);

    ReservationResponseDto releaseReservation(Long reservationId, String reason);

    ReservationResponseDto getReservationByOrderId(String orderId);
}

