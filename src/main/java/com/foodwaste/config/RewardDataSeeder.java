package com.foodwaste.config;

import com.foodwaste.model.Reward;
import com.foodwaste.model.RewardType;
import com.foodwaste.repo.RewardRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class RewardDataSeeder implements CommandLineRunner {
    private final RewardRepository rewards;
    private final JdbcTemplate jdbc;

    public RewardDataSeeder(RewardRepository rewards, JdbcTemplate jdbc) {
        this.rewards = rewards;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // Migrate an old reward value from earlier versions of the project.
        // The current application uses COUPON for discount-style rewards.
        jdbc.update("UPDATE rewards SET reward_type = ? WHERE reward_type = ?", "COUPON", "DISCOUNT");
        if (rewards.count() > 0) return;

        seed("Bronze Donor Badge", 20, RewardType.BADGE,
                "Awarded for your first 20 servings saved.", null);
        seed("Silver Donor Badge", 100, RewardType.BADGE,
                "Awarded for 100 servings saved.", null);
        seed("Gold Donor Badge", 300, RewardType.BADGE,
                "Awarded for 300 servings saved. Top-tier community donor.", null);
        seed("10% Off Grocery Coupon", 50, RewardType.COUPON,
                "Redeemable at partnered local grocery stores.", 10);
        seed("Impact Certificate", 150, RewardType.CERTIFICATE,
                "A certificate recognizing your contribution to reducing food waste.", null);
    }

    private void seed(String name, int points, RewardType type, String description, Integer value) {
        Reward r = new Reward();
        r.setName(name);
        r.setRequiredPoints(points);
        r.setRewardType(type);
        r.setDescription(description);
        r.setRewardValue(value);
        r.setActive(true);
        rewards.save(r);
    }
}
