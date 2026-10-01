package com.foodwaste.repo;

import com.foodwaste.model.FoodListing;
import com.foodwaste.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FoodListingRepository extends JpaRepository<FoodListing, Long> {
    List<FoodListing> findAllByOrderByIdDesc();

    List<FoodListing> findByStatusInAndExpiresAtBefore(Collection<ListingStatus> statuses, LocalDateTime time);

    long countByStatus(ListingStatus status);

    @Query("select coalesce(sum(l.servings), 0L) from FoodListing l where l.status = :status")
    Long sumServingsByStatus(@Param("status") ListingStatus status);

    // Receiver-facing: everyone browsing available food, plus an "expiring soon" slice of it
    List<FoodListing> findByStatusAndExpiresAtBetweenOrderByExpiresAtAsc(
            ListingStatus status, LocalDateTime from, LocalDateTime to);

    // Donor-facing: only this donor's own listings
    List<FoodListing> findByDonorEmailOrderByIdDesc(String donorEmail);

    long countByDonorEmail(String donorEmail);
    long countByDonorEmailAndStatus(String donorEmail, ListingStatus status);

    @Query("select coalesce(sum(l.servings), 0L) from FoodListing l where l.donor.email = :email and l.status = :status")
    Long sumServingsByDonorEmailAndStatus(@Param("email") String email, @Param("status") ListingStatus status);
}
