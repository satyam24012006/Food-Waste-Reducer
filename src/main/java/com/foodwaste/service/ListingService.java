package com.foodwaste.service;

import com.foodwaste.dto.Dtos.*;
import com.foodwaste.model.FoodListing;
import com.foodwaste.model.ListingStatus;
import com.foodwaste.model.User;
import com.foodwaste.repo.FoodListingRepository;
import com.foodwaste.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
public class ListingService {

    private final FoodListingRepository listings;
    private final UserRepository users;
    private final NotificationsService notificationsService;

    private final int pointsPerServing;
    private final int expiringSoonMinutes;
    private final double kgPerServing;

    public ListingService(
            FoodListingRepository listings,
            UserRepository users,
            NotificationsService notificationsService,
            @Value("${app.rewards.points-per-serving:1}") int pointsPerServing,
            @Value("${app.listings.expiring-soon-minutes:60}") int expiringSoonMinutes,
            @Value("${app.impact.kg-per-serving:0.25}") double kgPerServing) {

        this.listings = listings;
        this.users = users;
        this.notificationsService = notificationsService;
        this.pointsPerServing = pointsPerServing;
        this.expiringSoonMinutes = expiringSoonMinutes;
        this.kgPerServing = kgPerServing;
    }


    @Transactional(readOnly = true)
    public List<ListingResponse> all() {

        return listings.findAllByOrderByIdDesc()
                .stream()
                .map(ListingResponse::from)
                .toList();
    }



    /** Available food within a fixed 10 km radius. */
    @Transactional(readOnly = true)
    public List<ListingResponse> nearby(
            double latitude,
            double longitude) {

        final double radiusKm = 10.0;

        return listings
                .findByStatusAndExpiresAtBetweenOrderByExpiresAtAsc(
                        ListingStatus.AVAILABLE,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusYears(1))
                .stream()
                .filter(l ->
                        l.getPickupLatitude() != null &&
                                l.getPickupLongitude() != null)
                .map(l ->
                        new Object[]{
                                l,
                                distanceInKm(
                                        latitude,
                                        longitude,
                                        l.getPickupLatitude(),
                                        l.getPickupLongitude())
                        })
                .filter(x -> (double) x[1] <= radiusKm)
                .sorted(
                        Comparator.comparingDouble(
                                x -> (double) x[1]))
                .map(x ->
                        ListingResponse.from(
                                (FoodListing) x[0],
                                (double) x[1]))
                .toList();
    }

    private double distanceInKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double earthRadius = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        +
                        Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);

        return earthRadius
                * 2
                * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));
    }



    @Transactional(readOnly = true)
    public List<ListingResponse> expiringSoon() {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime soon =
                now.plusMinutes(expiringSoonMinutes);

        return listings
                .findByStatusAndExpiresAtBetweenOrderByExpiresAtAsc(
                        ListingStatus.AVAILABLE,
                        now,
                        soon)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }



    @Transactional(readOnly = true)
    public List<ListingResponse> mine(
            String donorEmail) {

        return listings
                .findByDonorEmailOrderByIdDesc(donorEmail)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }



    @Transactional
    public ListingResponse create(
            ListingRequest r,
            String donorEmail) {

        FoodListing l = new FoodListing();

        l.setTitle(r.title().trim());
        l.setDescription(r.description());
        l.setImageUrl(r.imageUrl());
        l.setServings(r.servings());
        l.setPickupLocation(r.pickupLocation().trim());
        l.setPickupLatitude(r.pickupLatitude());
        l.setPickupLongitude(r.pickupLongitude());
        l.setFoodType(r.foodType());
        l.setExpiresAt(r.expiresAt());
        l.setStatus(ListingStatus.AVAILABLE);
        l.setDonor(user(donorEmail));

        FoodListing saved = listings.save(l);



        notificationsService.notifyReceiversAboutNewFood();

        return ListingResponse.from(saved);
    }



    @Transactional
    public ListingResponse claim(
            Long id,
            String receiverEmail) {

        FoodListing l = find(id);

        if (l.getStatus() != ListingStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This food is no longer available");
        }

        if (l.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This listing has expired");
        }

        // Get receiver and donor
        User receiver = user(receiverEmail);
        User donor = l.getDonor();

        // Update listing
        l.setStatus(ListingStatus.CLAIMED);
        l.setClaimedBy(receiver);
        l.setClaimedAt(LocalDateTime.now());

        FoodListing saved = listings.save(l);



        notificationsService.notifyDonorFoodClaimed(
                donor,
                receiver);

        return ListingResponse.from(saved);
    }



    /** Receiver changes their mind. */
    @Transactional
    public ListingResponse cancelClaim(
            Long id,
            String receiverEmail) {

        FoodListing l = find(id);

        if (l.getStatus() != ListingStatus.CLAIMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only claimed listings can be cancelled");
        }

        if (l.getClaimedBy() == null
                || !l.getClaimedBy()
                .getEmail()
                .equals(receiverEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the receiver who claimed it can cancel");
        }

        l.setStatus(ListingStatus.AVAILABLE);
        l.setClaimedBy(null);
        l.setClaimedAt(null);

        return ListingResponse.from(l);
    }



    @Transactional
    public ListingResponse markPickedUp(
            Long id,
            String donorEmail) {

        FoodListing l = find(id);

        if (!l.getDonor()
                .getEmail()
                .equals(donorEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the donor can confirm pickup");
        }

        if (l.getStatus() != ListingStatus.CLAIMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only claimed listings can be marked picked up");
        }

        l.setStatus(ListingStatus.PICKED_UP);

        // Reward donor
        User donor = l.getDonor();

        donor.setRewardPoints(
                donor.getRewardPoints()
                        + l.getServings() * pointsPerServing);

        users.save(donor);

        return ListingResponse.from(l);
    }



    @Transactional
    public ListingResponse delete(
            Long id,
            String email,
            boolean isAdmin) {

        FoodListing l = find(id);

        if (!isAdmin
                && !l.getDonor()
                .getEmail()
                .equals(email)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only delete your own listings");
        }

        if (l.getStatus() != ListingStatus.AVAILABLE
                && !isAdmin) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only available listings can be deleted");
        }

        ListingResponse snapshot =
                ListingResponse.from(l);

        listings.delete(l);

        return snapshot;
    }


    @Transactional
    public List<ListingResponse> expireOverdue() {

        List<FoodListing> overdue =
                listings.findByStatusInAndExpiresAtBefore(
                        List.of(
                                ListingStatus.AVAILABLE,
                                ListingStatus.CLAIMED),
                        LocalDateTime.now());

        overdue.forEach(
                l -> l.setStatus(ListingStatus.EXPIRED));

        return overdue
                .stream()
                .map(ListingResponse::from)
                .toList();
    }


    @Transactional(readOnly = true)
    public Stats stats() {

        return new Stats(
                listings.count(),
                listings.countByStatus(
                        ListingStatus.AVAILABLE),
                listings.countByStatus(
                        ListingStatus.CLAIMED),
                listings.countByStatus(
                        ListingStatus.PICKED_UP),
                listings.countByStatus(
                        ListingStatus.EXPIRED),
                listings.sumServingsByStatus(
                        ListingStatus.PICKED_UP));
    }


    @Transactional(readOnly = true)
    public DonorStats donorStats(
            String donorEmail) {

        User donor = user(donorEmail);

        return new DonorStats(
                donor.getRewardPoints(),

                listings.countByDonorEmail(
                        donorEmail),

                listings.countByDonorEmailAndStatus(
                        donorEmail,
                        ListingStatus.CLAIMED),

                listings.countByDonorEmailAndStatus(
                        donorEmail,
                        ListingStatus.PICKED_UP),

                listings.countByDonorEmailAndStatus(
                        donorEmail,
                        ListingStatus.EXPIRED),

                listings.sumServingsByDonorEmailAndStatus(
                        donorEmail,
                        ListingStatus.PICKED_UP),

                listings.sumServingsByDonorEmailAndStatus(
                        donorEmail,
                        ListingStatus.PICKED_UP)
                        * kgPerServing);
    }


    @Transactional(readOnly = true)
    public ContactResponse contact(
            Long id,
            String email) {

        FoodListing l = find(id);

        User donor = l.getDonor();
        User receiver = l.getClaimedBy();

        if (receiver == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Contact is available after the food is claimed");
        }

        if (email.equals(receiver.getEmail())) {

            return new ContactResponse(
                    donor.getName(),
                    donor.getPhone());
        }

        if (email.equals(donor.getEmail())) {

            return new ContactResponse(
                    receiver.getName(),
                    receiver.getPhone());
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Not allowed to view this contact");
    }



    private FoodListing find(Long id) {

        return listings.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Listing not found"));
    }



    private User user(String email) {

        return users.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "User not found"));
    }
}