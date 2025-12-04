package com.kevdev.inventory.repository;

import com.kevdev.inventory.entity.ReservationLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationLineRepository extends JpaRepository<ReservationLine, Long> {
}

