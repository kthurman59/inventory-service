package com.kevdev.inventory.integration;

import com.kevdev.inventory.entity.InventoryItem;
import com.kevdev.inventory.entity.Reservation;
import com.kevdev.inventory.entity.ReservationLine;
import com.kevdev.inventory.messaging.event.InventoryReservationResultEvent;
import com.kevdev.inventory.messaging.event.OrderCreatedEvent;
import com.kevdev.inventory.messaging.event.OrderItemEvent;
import com.kevdev.inventory.repository.InventoryItemRepository;
import com.kevdev.inventory.repository.ReservationLineRepository;
import com.kevdev.inventory.repository.ReservationRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "inventory.kafka.enabled=true"
})
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 3,
        topics = {"orders.created", "inventory.reservation.results"}
)
class ReservationFlowIntegrationTest {

    private static final String TEST_SKU = "SKU_TEST";
    private static final String TEST_LOCATION = "MAIN";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationLineRepository reservationLineRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    private KafkaTemplate<String, OrderCreatedEvent> orderCreatedTemplate;

    private Consumer<String, InventoryReservationResultEvent> resultConsumer;

    @BeforeEach
    void setUp() {
        // Clean DB
        reservationLineRepository.deleteAll();
        reservationRepository.deleteAll();
        inventoryItemRepository.deleteAll();

        // Seed inventory row that the reservation logic expects
        InventoryItem inventoryItem = new InventoryItem(
                TEST_SKU,
                TEST_LOCATION,
                10,
                0
        );
        inventoryItemRepository.saveAndFlush(inventoryItem);

        // Producer for orders.created
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        DefaultKafkaProducerFactory<String, OrderCreatedEvent> pf =
                new DefaultKafkaProducerFactory<>(
                        producerProps,
                        new StringSerializer(),
                        new JsonSerializer<>()
                );

        orderCreatedTemplate = new KafkaTemplate<>(pf);

        // Consumer for inventory.reservation.results
        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("reservation-flow-test", "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<InventoryReservationResultEvent> valueDeserializer =
                new JsonDeserializer<>(InventoryReservationResultEvent.class, false);
        valueDeserializer.addTrustedPackages("com.kevdev.inventory.messaging.event");

        DefaultKafkaConsumerFactory<String, InventoryReservationResultEvent> cf =
                new DefaultKafkaConsumerFactory<>(
                        consumerProps,
                        new StringDeserializer(),
                        valueDeserializer
                );

        resultConsumer = cf.createConsumer();
        embeddedKafka.consumeFromEmbeddedTopics(resultConsumer, "inventory.reservation.results");
    }

    @AfterEach
    void tearDown() {
        if (resultConsumer != null) {
            resultConsumer.close();
        }
    }

    @Test
    void endToEnd_reservesInventory_andPublishesResult() {
        String orderId = "test-order-123";

        OrderCreatedEvent orderEvent = buildOrderCreatedEvent(orderId);

        // Send the order created event
        orderCreatedTemplate.send("orders.created", orderId, orderEvent);
        orderCreatedTemplate.flush();

        // Read the reservation result event
        var record = KafkaTestUtils.getSingleRecord(
                resultConsumer,
                "inventory.reservation.results",
                Duration.ofSeconds(20)
        );

        InventoryReservationResultEvent resultEvent = record.value();

        assertThat(resultEvent).isNotNull();
        assertThat(resultEvent.orderId()).isEqualTo(orderId);

        // Verify reservation exists in the database
        Optional<Reservation> reservationOpt = reservationRepository.findByOrderId(orderId);
        assertThat(reservationOpt).isPresent();

        Reservation reservation = reservationOpt.orElseThrow();
        assertThat(reservation.getStatus()).isNotNull();

        List<ReservationLine> lines =
                reservationLineRepository.findByReservationId(reservation.getId());

        assertThat(lines).isNotEmpty();

        ReservationLine line = lines.get(0);
        assertThat(line.getReservedQuantity()).isGreaterThan(0L);
    }

    private OrderCreatedEvent buildOrderCreatedEvent(String orderId) {
        Long productId = 1L;
        long requestedQuantity = 5L;

        OrderItemEvent item = new OrderItemEvent(
                productId,
                TEST_SKU,
                TEST_LOCATION,
                requestedQuantity
        );

        return new OrderCreatedEvent(
                orderId,
                List.of(item)
        );
    }
}

