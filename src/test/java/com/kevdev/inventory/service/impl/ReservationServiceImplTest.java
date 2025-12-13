package com.kevdev.inventory.service.impl;

import com.kevdev.inventory.dto.ReservationResponseDto;
import com.kevdev.inventory.entity.InventoryItem;
import com.kevdev.inventory.entity.Reservation;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final String TEST_SKU = "SKU_TEST";
    private static final String TEST_LOCATION = "MAIN";

    @Mock ReservationRepository reservationRepository;
    @Mock InventoryItemRepository inventoryItemRepository;

    @Mock ObjectProvider<KafkaTemplate<String, InventoryReservationResultEvent>> inventoryResultKafkaTemplateProvider;
    @Mock KafkaTemplate<String, InventoryReservationResultEvent> kafkaTemplate;

    private ReservationServiceImpl reservationService;

    @BeforeEach
    void setUp() {
        when(inventoryResultKafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);

        reservationService = new ReservationServiceImpl(
                reservationRepository,
                inventoryItemRepository,
                inventoryResultKafkaTemplateProvider
        );
    }

    @Test
    void reserveForOrder_whenInventoryAvailable_confirmsReservationAndSendsEvent() {
        String orderId = "ORDER_1";

        OrderCreatedEvent orderEvent = buildOrderCreatedEvent(orderId);

        InventoryItem inventoryItem = new InventoryItem(TEST_SKU, TEST_LOCATION, 10, 0);

        when(inventoryItemRepository.findBySkuAndLocationId(TEST_SKU, TEST_LOCATION))
                .thenReturn(Optional.of(inventoryItem));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<InventoryReservationResultEvent> eventCaptor =
                ArgumentCaptor.forClass(InventoryReservationResultEvent.class);

        ReservationResponseDto response = reservationService.reserveForOrder(orderEvent);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.status()).isEqualTo("CONFIRMED");

        assertThat(inventoryItem.getReservedQuantity()).isEqualTo(5L);

        verify(kafkaTemplate).send(
                eq("inventory.reservation.results"),
                eq(orderId),
                eventCaptor.capture()
        );

        InventoryReservationResultEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent).isNotNull();
        assertThat(sentEvent.orderId()).isEqualTo(orderId);
        assertThat(sentEvent.status()).isEqualTo("CONFIRMED");
    }

    private OrderCreatedEvent buildOrderCreatedEvent(String orderId) {
        long requestedQuantity = 5L;

        OrderItemEvent item = new OrderItemEvent(
                1L,
                TEST_SKU,
                TEST_LOCATION,
                requestedQuantity
        );

        return new OrderCreatedEvent(orderId, List.of(item));
    }
}

