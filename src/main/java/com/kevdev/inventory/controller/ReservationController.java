package com.kevdev.inventory.controller;

import com.kevdev.inventory.dto.ReservationActionRequestDto;
import com.kevdev.inventory.dto.ReservationCreateRequestDto;
import com.kevdev.inventory.dto.ReservationResponseDto;
import com.kevdev.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // POST  /api/inventory/reservations
    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(
            @Valid @RequestBody ReservationCreateRequestDto request
    ) {
        ReservationResponseDto response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST  /api/inventory/reservations/{reservationId}/commit
    @PostMapping("/{reservationId}/commit")
    public ReservationResponseDto commitReservation(
            @PathVariable("reservationId") Long reservationId,
            @RequestBody ReservationActionRequestDto request
    ) {
        return reservationService.commitReservation(reservationId, request.reason());
    }

    // POST  /api/inventory/reservations/{reservationId}/release
    @PostMapping("/{reservationId}/release")
    public ReservationResponseDto releaseReservation(
            @PathVariable("reservationId") Long reservationId,
            @RequestBody ReservationActionRequestDto request
    ) {
        return reservationService.releaseReservation(reservationId, request.reason());
    }

    // GET  /api/inventory/reservations/order/{orderId}
    @GetMapping("/by-order/{orderId}")
    public ReservationResponseDto getReservationByOrderId(
            @PathVariable("orderId") String orderId
    ) {
        return reservationService.getReservationByOrderId(orderId);
    }
}

