package com.kevdev.inventory.controller;

import com.kevdev.inventory.dto.ReservationActionRequestDto;
import com.kevdev.inventory.dto.ReservationCreateRequestDto;
import com.kevdev.inventory.dto.ReservationResponseDto;
import com.kevdev.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponseDto> createReservation(
            @Valid @RequestBody ReservationCreateRequestDto request
    ) {
        ReservationResponseDto response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/reservations/{reservationId}/commit")
    public ReservationResponseDto commitReservation(
            @PathVariable("reservationId") Long reservationId,
            @RequestBody ReservationActionRequestDto request
    ) {
        return reservationService.commitReservation(reservationId, request.reason());
    }

    @PostMapping("/reservations/{reservationId}/release")
    public ReservationResponseDto releaseReservation(
            @PathVariable("reservationId") Long reservationId,
            @RequestBody ReservationActionRequestDto request
    ) {
        return reservationService.releaseReservation(reservationId, request.reason());
    }

    @GetMapping("/reservations/by-order/{orderId}")
    public ReservationResponseDto getReservationByOrderId(
            @PathVariable("orderId") String orderId
    ) {
        return reservationService.getReservationByOrderId(orderId);
    }
}

