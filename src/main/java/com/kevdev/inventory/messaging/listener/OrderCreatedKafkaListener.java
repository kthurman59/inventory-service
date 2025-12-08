package com.kevdev.inventory.messaging.listener;

import com.kevdev.inventory.messaging.event.OrderCreatedEvent;
import com.kevdev.inventory.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inventory.kafka.enabled", havingValue = "true")
public class OrderCreatedKafkaListener {

    private final ReservationService reservationService;

    @KafkaListener(topics = "orders.created", groupId = "inventory-service")
    public void handleOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId={}", event.orderId());
        reservationService.reserveForOrder(event);
    }
}

