package com.example.stayfinder.backend.repository;



import com.example.stayfinder.backend.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository
        extends JpaRepository<Review, UUID> {

    List<Review> findByPropertyId(UUID propertyId);

    List<Review> findByGuestId(UUID guestId);

    boolean existsByBookingId(UUID bookingId);

    // Calculate average rating for a property
    @Query("SELECT AVG(r.rating) FROM Review r " +
            "WHERE r.property.id = :propertyId")
    Optional<Double> findAvgRatingByPropertyId(
            @Param("propertyId") UUID propertyId);
}