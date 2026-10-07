package com.foodwaste.repo;

import com.foodwaste.model.Notifications;
import com.foodwaste.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationsRepository extends JpaRepository<Notifications, Long> {

    List<Notifications> findByRecipientOrderByCreatedAtDesc(User recipient);
}