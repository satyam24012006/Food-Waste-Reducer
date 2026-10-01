package com.foodwaste.service;

import com.foodwaste.dto.Dtos.*;
import com.foodwaste.model.Reward;
import com.foodwaste.model.User;
import com.foodwaste.model.UserReward;
import com.foodwaste.repo.RewardRepository;
import com.foodwaste.repo.UserRepository;
import com.foodwaste.repo.UserRewardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RewardService {
    private final RewardRepository rewards;
    private final UserRepository users;
    private final UserRewardRepository userRewards;

    public RewardService(RewardRepository rewards, UserRepository users, UserRewardRepository userRewards) {
        this.rewards = rewards;
        this.users = users;
        this.userRewards = userRewards;
    }

    @Transactional(readOnly = true)
    public List<RewardResponse> catalog() {
        return rewards.findByActiveTrueOrderByRequiredPointsAsc().stream().map(RewardResponse::from).toList();
    }

    /** Redeems a reward for the currently authenticated user (identified by email, not a client-supplied id). */
    @Transactional
    public RedeemResponse redeem(String email, Long rewardId) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Reward reward = rewards.findById(rewardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reward not found"));

        if (!reward.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reward is no longer active");
        }
        if (user.getRewardPoints() < reward.getRequiredPoints()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough points for this reward");
        }

        user.setRewardPoints(user.getRewardPoints() - reward.getRequiredPoints());
        users.save(user);

        String couponCode = "FWR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        UserReward ur = new UserReward();
        ur.setUser(user);
        ur.setReward(reward);
        ur.setCouponCode(couponCode);
        ur.setRedeemedAt(LocalDateTime.now());
        userRewards.save(ur);

        return new RedeemResponse(reward.getName(), couponCode, user.getRewardPoints());
    }

    @Transactional(readOnly = true)
    public List<UserReward> myRedemptions(String email) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return userRewards.findByUserOrderByRedeemedAtDesc(user);
    }
}
