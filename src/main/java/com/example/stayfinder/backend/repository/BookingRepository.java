package com.example.stayfinder.backend.repository;



import com.example.stayfinder.backend.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Find all bookings for a property
    List<Booking> findByPropertyId(UUID propertyId);

    // Find all bookings for a guest
    List<Booking> findByGuestId(UUID guestId);

    // Check if dates overlap with existing CONFIRMED bookings
    // This is the core date conflict detection query
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.property.id = :propertyId
        AND b.status = 'CONFIRMED'
        AND b.checkIn < :checkOut
        AND b.checkOut > :checkIn
        """)
    boolean existsOverlappingBooking(
            @Param("propertyId") UUID propertyId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}