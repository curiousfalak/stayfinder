package com.example.stayfinder.backend.repository;

import com.example.stayfinder.backend.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingStatusHistoryRepository
        extends JpaRepository<BookingStatusHistory, UUID> {

    List<BookingStatusHistory> findByBookingId(UUID bookingId);
}