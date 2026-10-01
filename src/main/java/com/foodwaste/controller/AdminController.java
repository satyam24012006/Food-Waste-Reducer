package com.foodwaste.controller;

import com.foodwaste.dto.Dtos.AdminStats;
import com.foodwaste.model.Role;
import com.foodwaste.model.ListingStatus;
import com.foodwaste.repo.FoodListingRepository;
import com.foodwaste.repo.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final FoodListingRepository listings;

    public AdminController(UserRepository users, FoodListingRepository listings) {
        this.users = users;
        this.listings = listings;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return new AdminStats(
                users.count(), users.countByRole(Role.DONOR), users.countByRole(Role.RECEIVER),
                listings.count(), listings.countByStatus(ListingStatus.AVAILABLE),
                listings.countByStatus(ListingStatus.CLAIMED), listings.countByStatus(ListingStatus.PICKED_UP),
                listings.countByStatus(ListingStatus.EXPIRED),
                listings.sumServingsByStatus(ListingStatus.PICKED_UP));
    }
}
