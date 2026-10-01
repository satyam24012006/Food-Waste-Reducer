package com.foodwaste.repo;

import com.foodwaste.model.User;
import com.foodwaste.model.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRewardRepository extends JpaRepository<UserReward, Long> {
    List<UserReward> findByUserOrderByRedeemedAtDesc(User user);
}
