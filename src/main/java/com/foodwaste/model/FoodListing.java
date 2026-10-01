package com.foodwaste.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_listings")
@Getter @Setter @NoArgsConstructor
public class FoodListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optimistic lock: stops two receivers from claiming the same listing at once. */
    @Version
    private Long version;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    private int servings;

    private String pickupLocation;

    private Double pickupLatitude;
    private Double pickupLongitude;

    @Enumerated(EnumType.STRING)
    private FoodType foodType;

    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private ListingStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donor_id")
    private User donor;

    @ManyToOne
    @JoinColumn(name = "claimed_by_id")
    private User claimedBy;

    private LocalDateTime claimedAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
