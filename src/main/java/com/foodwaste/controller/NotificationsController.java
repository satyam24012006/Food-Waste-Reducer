package com.foodwaste.controller;

import com.foodwaste.model.Notifications;
import com.foodwaste.model.User;
import com.foodwaste.repo.UserRepository;
import com.foodwaste.service.NotificationsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {

    private final NotificationsService notificationsService;
    private final UserRepository userRepository;

    public NotificationsController(
            NotificationsService notificationsService,
            UserRepository userRepository) {

        this.notificationsService = notificationsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Notifications>> getMyNotifications(
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        return ResponseEntity.ok(
                notificationsService.getMyNotifications(user)
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        notificationsService.markAsRead(id, user);

        return ResponseEntity.ok().build();
    }
}
