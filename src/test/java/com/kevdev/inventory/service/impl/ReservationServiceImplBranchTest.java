package com.kevdev.inventory.service.impl;

import com.kevdev.inventory.dto.ReservationCreateRequestDto;
import com.kevdev.inventory.dto.ReservationItemRequestDto;
import com.kevdev.inventory.dto.ReservationResponseDto;
import com.kevdev.inventory.entity.InventoryItem;
import com.kevdev.inventory.entity.Reservation;
import com.kevdev.inventory.entity.ReservationLine;
import com.kevdev.inventory.entity.ReservationLineStatus;
import com.kevdev.inventory.entity.ReservationStatus;
import com.kevdev.inventory.messaging.event.InventoryReservationResultEvent;
import com.kevdev.inventory.messaging.event.OrderCreatedEvent;
import com.kevdev.inventory.messaging.event.OrderItemEvent;
import com.kevdev.inventory.repository.InventoryItemRepository;
import com.kevdev.inventory.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplBranchTest {

    private static final String TOPIC_RESULTS = "inventory.reservation.results";

    @Mock ReservationRepository reservationRepository;
    @Mock InventoryItemRepository inventoryItemRepository;

    @Mock ObjectProvider<KafkaTemplate<String, InventoryReservationResultEvent>> inventoryResultKafkaTemplateProvider;
    @Mock KafkaTemplate<String, InventoryReservationResultEvent> kafkaTemplate;

    private ReservationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReservationServiceImpl(
                reservationRepository,
                inventoryItemRepository,
                inventoryResultKafkaTemplateProvider
        );
    }

    @Test
    void reserveForOrder_whenTemplateUnavailable_doesNotSendEvent() {
        when(inventoryResultKafkaTemplateProvider.getIfAvailable()).thenReturn(null);

        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 0);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreatedEvent orderEvent = new OrderCreatedEvent(
                "ORDER_1",
                List.of(new OrderItemEvent(1L, "SKU1", "MAIN", 5L))
        );

        ReservationResponseDto response = service.reserveForOrder(orderEvent);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void createReservation_whenAllItemsReserved_setsConfirmed() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 0);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationCreateRequestDto request = new ReservationCreateRequestDto(
                "ORDER_1",
                List.of(new ReservationItemRequestDto("SKU1", "MAIN", 5L))
        );

        ReservationResponseDto response = service.createReservation(request);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(item.getReservedQuantity()).isEqualTo(5L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).status()).isEqualTo(ReservationLineStatus.RESERVED.name());
    }

    @Test
    void createReservation_whenAllItemsFail_setsFailedAndReason() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 2, 0);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationCreateRequestDto request = new ReservationCreateRequestDto(
                "ORDER_1",
                List.of(new ReservationItemRequestDto("SKU1", "MAIN", 5L))
        );

        ReservationResponseDto response = service.createReservation(request);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.reason()).isEqualTo("All items failed reservation");
        assertThat(item.getReservedQuantity()).isEqualTo(0L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).status()).isEqualTo(ReservationLineStatus.FAILED.name());
        assertThat(response.items().get(0).failureReason()).isEqualTo("Insufficient available stock");
    }

    @Test
    void createReservation_whenMixedItems_setsPartial() {
        InventoryItem ok = new InventoryItem("SKU_OK", "MAIN", 10, 0);
        InventoryItem bad = new InventoryItem("SKU_BAD", "MAIN", 2, 0);

        when(inventoryItemRepository.findBySkuAndLocationId("SKU_OK", "MAIN"))
                .thenReturn(Optional.of(ok));
        when(inventoryItemRepository.findBySkuAndLocationId("SKU_BAD", "MAIN"))
                .thenReturn(Optional.of(bad));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationCreateRequestDto request = new ReservationCreateRequestDto(
                "ORDER_1",
                List.of(
                        new ReservationItemRequestDto("SKU_OK", "MAIN", 5L),
                        new ReservationItemRequestDto("SKU_BAD", "MAIN", 5L)
                )
        );

        ReservationResponseDto response = service.createReservation(request);

        assertThat(response.status()).isEqualTo("PARTIAL");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().stream().anyMatch(i -> i.status().equals("RESERVED"))).isTrue();
        assertThat(response.items().stream().anyMatch(i -> i.status().equals("FAILED"))).isTrue();
        assertThat(ok.getReservedQuantity()).isEqualTo(5L);
        assertThat(bad.getReservedQuantity()).isEqualTo(0L);
    }

    @Test
    void createReservation_whenLocationMissing_usesDefaultMain() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 0);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationCreateRequestDto request = new ReservationCreateRequestDto(
                "ORDER_1",
                List.of(new ReservationItemRequestDto("SKU1", null, 5L))
        );

        ReservationResponseDto response = service.createReservation(request);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(inventoryItemRepository).findBySkuAndLocationId(eq("SKU1"), eq("MAIN"));
    }

    @Test
    void reserveForOrder_sendsResultEvent_withLines() {
        when(inventoryResultKafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);

        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 0);
        when(inventoryItemRepository.findBySkuAndLocationId("SKU1", "MAIN"))
                .thenReturn(Optional.of(item));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreatedEvent orderEvent = new OrderCreatedEvent(
                "ORDER_1",
                List.of(new OrderItemEvent(1L, "SKU1", "MAIN", 5L))
        );

        ArgumentCaptor<InventoryReservationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(InventoryReservationResultEvent.class);

        ReservationResponseDto response = service.reserveForOrder(orderEvent);

        assertThat(response.status()).isEqualTo("CONFIRMED");

        verify(kafkaTemplate).send(eq(TOPIC_RESULTS), eq("ORDER_1"), eventCaptor.capture());

        InventoryReservationResultEvent sent = eventCaptor.getValue();
        assertThat(sent.orderId()).isEqualTo("ORDER_1");
        assertThat(sent.status()).isEqualTo("CONFIRMED");
        assertThat(sent.lines()).hasSize(1);
        assertThat(sent.lines().get(0).sku()).isEqualTo("SKU1");
        assertThat(sent.lines().get(0).status()).isEqualTo("RESERVED");
    }

    @Test
    void commitReservation_whenStatusInvalid_throws() {
        Reservation reservation = newReservation(1L, "ORDER_1", ReservationStatus.PENDING, List.of());

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> service.commitReservation(1L, "reason"));

        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    @Test
    void commitReservation_happyPath_updatesOnHandAndReserved_andSetsCommitted() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 5);
        Reservation reservation = newReservation(1L, "ORDER_1", ReservationStatus.CONFIRMED, List.of(
                newLine(reservationStub(), item, 5L, ReservationLineStatus.RESERVED)
        ));

        fixLineReservationBackrefs(reservation);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDto response = service.commitReservation(1L, "ok");

        assertThat(response.status()).isEqualTo("COMMITTED");
        assertThat(item.getOnHandQuantity()).isEqualTo(5L);
        assertThat(item.getReservedQuantity()).isEqualTo(0L);
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void releaseReservation_happyPath_updatesReserved_andSetsReleased() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 5);
        Reservation reservation = newReservation(1L, "ORDER_1", ReservationStatus.CONFIRMED, List.of(
                newLine(reservationStub(), item, 5L, ReservationLineStatus.RESERVED)
        ));

        fixLineReservationBackrefs(reservation);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDto response = service.releaseReservation(1L, "ok");

        assertThat(response.status()).isEqualTo("RELEASED");
        assertThat(item.getOnHandQuantity()).isEqualTo(10L);
        assertThat(item.getReservedQuantity()).isEqualTo(0L);
        verify(inventoryItemRepository).save(any(InventoryItem.class));
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void commitReservation_whenCommitWouldMakeOnHandNegative_throws() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 3, 5);
        Reservation reservation = newReservation(1L, "ORDER_1", ReservationStatus.CONFIRMED, List.of(
                newLine(reservationStub(), item, 5L, ReservationLineStatus.RESERVED)
        ));

        fixLineReservationBackrefs(reservation);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> service.commitReservation(1L, "ok"));
    }

    @Test
    void commitReservation_whenCommitWouldMakeReservedNegative_throws() {
        InventoryItem item = new InventoryItem("SKU1", "MAIN", 10, 3);
        Reservation reservation = newReservation(1L, "ORDER_1", ReservationStatus.CONFIRMED, List.of(
                newLine(reservationStub(), item, 5L, ReservationLineStatus.RESERVED)
        ));

        fixLineReservationBackrefs(reservation);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> service.commitReservation(1L, "ok"));
    }

    private Reservation newReservation(Long id, String orderId, ReservationStatus status, List<ReservationLine> lines) {
        Reservation reservation = Reservation.builder()
                .orderId(orderId)
                .status(status)
                .reason(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .expiresAt(null)
                .build();

        ReflectionTestUtils.setField(reservation, "id", id);

        Object existing = ReflectionTestUtils.getField(reservation, "lines");
        if (existing == null) {
            ReflectionTestUtils.setField(reservation, "lines", new ArrayList<ReservationLine>());
        }

        @SuppressWarnings("unchecked")
        List<ReservationLine> targetLines = (List<ReservationLine>) ReflectionTestUtils.getField(reservation, "lines");
        targetLines.clear();
        targetLines.addAll(lines);

        return reservation;
    }

    private ReservationLine newLine(Reservation reservation, InventoryItem item, long reservedQty, ReservationLineStatus status) {
        return ReservationLine.builder()
                .reservation(reservation)
                .inventoryItem(item)
                .sku(item.getSku())
                .locationId(item.getLocationId())
                .requestedQuantity(reservedQty)
                .reservedQuantity(reservedQty)
                .status(status)
                .failureReason(null)
                .build();
    }

    private Reservation reservationStub() {
        return Reservation.builder()
                .orderId("stub")
                .status(ReservationStatus.CONFIRMED)
                .reason(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .expiresAt(null)
                .build();
    }

    private void fixLineReservationBackrefs(Reservation reservation) {
        @SuppressWarnings("unchecked")
        List<ReservationLine> lines = (List<ReservationLine>) ReflectionTestUtils.getField(reservation, "lines");
        if (lines == null) {
            return;
        }
        for (ReservationLine line : lines) {
            line.setReservation(reservation);
        }
    }
}

