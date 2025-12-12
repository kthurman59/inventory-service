package com.kevdev.inventory.repository;

import com.kevdev.inventory.entity.ReservationLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationLineRepository extends JpaRepository<ReservationLine, Long> {

    List<ReservationLine> findByReservationId(Long reservationId);
}

