package com.kevdev.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevdev.inventory.dto.ReservationActionRequestDto;
import com.kevdev.inventory.dto.ReservationResponseDto;
import com.kevdev.inventory.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createReservation_returnsCreatedReservation() throws Exception {
        ReservationResponseDto response = new ReservationResponseDto(
                1L,
                "ORDER123",
                "PENDING",
                null,
                List.of() // we do not care about item details here
        );

        given(reservationService.createReservation(any())).willReturn(response);

        String requestJson = """
            {
              "orderId": "ORDER123",
              "items": [
                {
                  "sku": "SKU123",
                  "locationId": "LOC1",
                  "requestedQuantity": 5,
                  "requestedBy": "SYSTEM",
                  "requestedByType": "SERVICE",
                  "reason": "Initial reservation"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(1L))
                .andExpect(jsonPath("$.orderId").value("ORDER123"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(reservationService).createReservation(any());
    }

    @Test
    void commitReservation_returnsUpdatedReservation() throws Exception {
        ReservationResponseDto response = new ReservationResponseDto(
                1L,
                "ORDER123",
                "COMMITTED",
                "Committed successfully",
                List.of()
        );

        given(reservationService.commitReservation(eq(1L), anyString()))
                .willReturn(response);

        ReservationActionRequestDto requestDto =
                new ReservationActionRequestDto("Committed successfully");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/inventory/reservations/{reservationId}/commit", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(1L))
                .andExpect(jsonPath("$.status").value("COMMITTED"));

        verify(reservationService).commitReservation(eq(1L), eq("Committed successfully"));
    }

    @Test
    void releaseReservation_returnsUpdatedReservation() throws Exception {
        ReservationResponseDto response = new ReservationResponseDto(
                1L,
                "ORDER123",
                "RELEASED",
                "Released by request",
                List.of()
        );

        given(reservationService.releaseReservation(eq(1L), anyString()))
                .willReturn(response);

        ReservationActionRequestDto requestDto =
                new ReservationActionRequestDto("Released by request");

        String requestJson = objectMapper.writeValueAsString(requestDto);

        mockMvc.perform(post("/api/inventory/reservations/{reservationId}/release", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(1L))
                .andExpect(jsonPath("$.status").value("RELEASED"));

        verify(reservationService).releaseReservation(eq(1L), eq("Released by request"));
    }

    @Test
    void getReservationByOrderId_returnsReservation() throws Exception {
        ReservationResponseDto response = new ReservationResponseDto(
                1L,
                "ORDER123",
                "PENDING",
                null,
                List.of()
        );

        given(reservationService.getReservationByOrderId("ORDER123"))
                .willReturn(response);

        mockMvc.perform(get("/api/inventory/reservations/by-order/{orderId}", "ORDER123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(1L))
                .andExpect(jsonPath("$.orderId").value("ORDER123"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(reservationService).getReservationByOrderId("ORDER123");
    }
}

