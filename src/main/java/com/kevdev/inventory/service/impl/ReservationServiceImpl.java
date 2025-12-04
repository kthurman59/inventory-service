package com.kevdev.inventory.service.impl;

import com.kevdev.inventory.dto.ReservationCreateRequestDto;
import com.kevdev.inventory.dto.ReservationItemRequestDto;
import com.kevdev.inventory.dto.ReservationItemResponseDto;
import com.kevdev.inventory.dto.ReservationResponseDto;
import com.kevdev.inventory.entity.InventoryItem;
import com.kevdev.inventory.entity.Reservation;
import com.kevdev.inventory.entity.ReservationLine;
import com.kevdev.inventory.entity.ReservationLineStatus;
import com.kevdev.inventory.entity.ReservationStatus;
import com.kevdev.inventory.repository.InventoryItemRepository;
import com.kevdev.inventory.repository.ReservationRepository;
import com.kevdev.inventory.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private static final String DEFAULT_LOCATION_ID = "MAIN";

    private final ReservationRepository reservationRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional
    public ReservationResponseDto createReservation(ReservationCreateRequestDto request) {
        Instant now = Instant.now();

        Reservation reservation = Reservation.builder()
                .orderId(request.orderId())
                .status(ReservationStatus.PENDING)
                .reason(null)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(null)
                .build();

        for (ReservationItemRequestDto itemRequest : request.items()) {
            String locationId = resolveLocationId(itemRequest.locationId());

            InventoryItem inventoryItem = inventoryItemRepository
                    .findBySkuAndLocationId(itemRequest.sku(), locationId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Inventory item not found for sku " + itemRequest.sku() + " at location " + locationId
                    ));

            long requestedQuantity = itemRequest.quantity();
            long available = inventoryItem.getOnHandQuantity() - inventoryItem.getReservedQuantity();

            ReservationLine line;

            if (available >= requestedQuantity) {
                long newReserved = inventoryItem.getReservedQuantity() + requestedQuantity;
                inventoryItem.setReservedQuantity(newReserved);
                inventoryItem.setUpdatedAt(now);

                line = ReservationLine.builder()
                        .reservation(reservation)
                        .inventoryItem(inventoryItem)
                        .sku(inventoryItem.getSku())
                        .locationId(inventoryItem.getLocationId())
                        .requestedQuantity(requestedQuantity)
                        .reservedQuantity(requestedQuantity)
                        .status(ReservationLineStatus.RESERVED)
                        .failureReason(null)
                        .build();
            } else {
                line = ReservationLine.builder()
                        .reservation(reservation)
                        .inventoryItem(inventoryItem)
                        .sku(inventoryItem.getSku())
                        .locationId(inventoryItem.getLocationId())
                        .requestedQuantity(requestedQuantity)
                        .reservedQuantity(0L)
                        .status(ReservationLineStatus.FAILED)
                        .failureReason("Insufficient available stock")
                        .build();
            }

            reservation.getLines().add(line);
        }

        boolean anyReserved = reservation.getLines().stream()
                .anyMatch(line -> line.getStatus() == ReservationLineStatus.RESERVED);
        boolean anyFailed = reservation.getLines().stream()
                .anyMatch(line -> line.getStatus() == ReservationLineStatus.FAILED);

        if (anyReserved && anyFailed) {
            reservation.setStatus(ReservationStatus.PARTIAL);
        } else if (anyReserved) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        } else {
            reservation.setStatus(ReservationStatus.FAILED);
            reservation.setReason("All items failed reservation");
        }

        Reservation savedReservation = reservationRepository.save(reservation);

        return mapToReservationResponse(savedReservation);
    }

    @Override
    @Transactional
    public ReservationResponseDto commitReservation(Long reservationId, String reason) {
        Instant now = Instant.now();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with id " + reservationId));

        if (!(reservation.getStatus() == ReservationStatus.CONFIRMED
                || reservation.getStatus() == ReservationStatus.PARTIAL)) {
            throw new IllegalStateException("Cannot commit reservation in status " + reservation.getStatus());
        }

        for (ReservationLine line : reservation.getLines()) {
            long reservedQuantity = line.getReservedQuantity();
            if (reservedQuantity <= 0) {
                continue;
            }

            InventoryItem inventoryItem = line.getInventoryItem();

            long newOnHand = inventoryItem.getOnHandQuantity() - reservedQuantity;
            if (newOnHand < 0) {
                throw new IllegalStateException("Commit would reduce on hand below zero for sku "
                        + inventoryItem.getSku());
            }

            long newReserved = inventoryItem.getReservedQuantity() - reservedQuantity;
            if (newReserved < 0) {
                throw new IllegalStateException("Commit would reduce reserved below zero for sku "
                        + inventoryItem.getSku());
            }

            inventoryItem.setOnHandQuantity(newOnHand);
            inventoryItem.setReservedQuantity(newReserved);
            inventoryItem.setUpdatedAt(now);
            inventoryItemRepository.save(inventoryItem);
        }

        reservation.setStatus(ReservationStatus.COMMITTED);
        reservation.setReason(reason);
        reservation.setUpdatedAt(now);

        Reservation saved = reservationRepository.save(reservation);
        return mapToReservationResponse(saved);
    }

    @Override
    @Transactional
    public ReservationResponseDto releaseReservation(Long reservationId, String reason) {
        Instant now = Instant.now();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found with id " + reservationId));

        if (!(reservation.getStatus() == ReservationStatus.CONFIRMED
                || reservation.getStatus() == ReservationStatus.PARTIAL)) {
            throw new IllegalStateException("Cannot release reservation in status " + reservation.getStatus());
        }

        for (ReservationLine line : reservation.getLines()) {
            long reservedQuantity = line.getReservedQuantity();
            if (reservedQuantity <= 0) {
                continue;
            }

            InventoryItem inventoryItem = line.getInventoryItem();

            long newReserved = inventoryItem.getReservedQuantity() - reservedQuantity;
            if (newReserved < 0) {
                throw new IllegalStateException("Release would reduce reserved below zero for sku "
                        + inventoryItem.getSku());
            }

            inventoryItem.setReservedQuantity(newReserved);
            inventoryItem.setUpdatedAt(now);
            inventoryItemRepository.save(inventoryItem);
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReason(reason);
        reservation.setUpdatedAt(now);

        Reservation saved = reservationRepository.save(reservation);
        return mapToReservationResponse(saved);
    }

    @Override
    public ReservationResponseDto getReservationByOrderId(String orderId) {
        Reservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found for orderId " + orderId));
        return mapToReservationResponse(reservation);
    }

    private String resolveLocationId(String locationId) {
        if (locationId == null || locationId.isBlank()) {
            return DEFAULT_LOCATION_ID;
        }
        return locationId;
    }

    private ReservationResponseDto mapToReservationResponse(Reservation reservation) {
        List<ReservationItemResponseDto> itemDtos = reservation.getLines().stream()
                .map(line -> new ReservationItemResponseDto(
                        line.getSku(),
                        line.getLocationId(),
                        line.getRequestedQuantity(),
                        line.getReservedQuantity(),
                        line.getStatus().name(),
                        line.getFailureReason()
                ))
                .toList();

        return new ReservationResponseDto(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getStatus().name(),
                reservation.getReason(),
                itemDtos
        );
    }
}

