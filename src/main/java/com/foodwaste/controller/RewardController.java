package com.foodwaste.controller;

import com.foodwaste.dto.Dtos.*;
import com.foodwaste.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    private final RewardService service;

    public RewardController(RewardService service) {
        this.service = service;
    }

    @GetMapping
    public List<RewardResponse> catalog() {
        return service.catalog();
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasRole('DONOR')")
    public RedeemResponse redeem(
            @Valid @RequestBody RedeemRequest r,
            Authentication auth) {

        return service.redeem(
                auth.getName(),
                r.rewardId()
        );
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('DONOR')")
    public List<RedeemedRewardResponse> mine(
            Authentication auth) {

        return service.myRedemptions(auth.getName());
    }
}