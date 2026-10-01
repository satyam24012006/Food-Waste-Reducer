package com.foodwaste.dto;

import com.foodwaste.model.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class Dtos {

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password,
            @NotBlank @Pattern(regexp = "\\d{10}", message = "must be a 10-digit number") String phone,
            @NotNull Role role) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, Long id, String name, String email, Role role, Integer rewardPoints) {}

    public record ListingRequest(
            @NotBlank String title,
            String description,
            String imageUrl,
            @Min(value = 1, message = "must be at least 1") int servings,
            @NotBlank String pickupLocation,
            Double pickupLatitude,
            Double pickupLongitude,
            @NotNull FoodType foodType,
            @NotNull @Future(message = "must be in the future") LocalDateTime expiresAt) {}

    public record ListingResponse(
            Long id, String title, String description, String imageUrl, int servings, String pickupLocation,
            Double pickupLatitude, Double pickupLongitude,
            FoodType foodType, LocalDateTime expiresAt, ListingStatus status,
            Long donorId, String donorName, Long claimedById, String claimedByName, LocalDateTime createdAt, Double distanceKm) {

        public static ListingResponse from(FoodListing l) {
            return new ListingResponse(
                    l.getId(), l.getTitle(), l.getDescription(), l.getImageUrl(), l.getServings(), l.getPickupLocation(),
                    l.getPickupLatitude(), l.getPickupLongitude(),
                    l.getFoodType(), l.getExpiresAt(), l.getStatus(),
                    l.getDonor().getId(), l.getDonor().getName(),
                    l.getClaimedBy() == null ? null : l.getClaimedBy().getId(),
                    l.getClaimedBy() == null ? null : l.getClaimedBy().getName(),
                    l.getCreatedAt(), null);
        }

        public static ListingResponse from(FoodListing l, double distanceKm) {
            return new ListingResponse(
                    l.getId(), l.getTitle(), l.getDescription(), l.getImageUrl(), l.getServings(), l.getPickupLocation(),
                    l.getPickupLatitude(), l.getPickupLongitude(),
                    l.getFoodType(), l.getExpiresAt(), l.getStatus(),
                    l.getDonor().getId(), l.getDonor().getName(),
                    l.getClaimedBy() == null ? null : l.getClaimedBy().getId(),
                    l.getClaimedBy() == null ? null : l.getClaimedBy().getName(),
                    l.getCreatedAt(), distanceKm);
        }
    }

    /** The other party's contact details (never broadcast, fetched on demand). */
    public record ContactResponse(String name, String phone) {}

    /** Message pushed over WebSocket. type = NEW | CLAIMED | CANCELLED | PICKED_UP | EXPIRED | DELETED */
    public record ListingEvent(String type, ListingResponse listing) {}

    public record Stats(long total, long available, long claimed, long pickedUp, long expired, long servingsSaved) {}

    public record DonorStats(
            Integer points, long totalPosted, long totalClaimed,
            long totalPickedUp, long totalExpired, long servingsSaved, double estimatedKgSaved) {}

    public record AdminStats(long totalUsers, long donors, long receivers, long totalListings,
                             long available, long claimed, long pickedUp, long expired, long servingsSaved) {}

    public record RedeemRequest(@NotNull Long rewardId) {}

    public record RewardResponse(
            Long id, String name, Integer requiredPoints, RewardType rewardType,
            String description, Integer rewardValue, boolean active) {

        public static RewardResponse from(Reward r) {
            return new RewardResponse(r.getId(), r.getName(), r.getRequiredPoints(), r.getRewardType(),
                    r.getDescription(), r.getRewardValue(), r.isActive());
        }
    }

    public record RedeemResponse(String rewardName, String couponCode, Integer pointsRemaining) {}

    public record RedeemedRewardResponse(String rewardName, String couponCode, java.time.LocalDateTime redeemedAt) {
        public static RedeemedRewardResponse from(UserReward ur) {
            return new RedeemedRewardResponse(ur.getReward().getName(), ur.getCouponCode(), ur.getRedeemedAt());
        }
    }
}
