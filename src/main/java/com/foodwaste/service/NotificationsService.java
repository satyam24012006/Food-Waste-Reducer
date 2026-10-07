package com.foodwaste.service;

import com.foodwaste.model.Notifications;
import com.foodwaste.model.Role;
import com.foodwaste.model.User;
import com.foodwaste.repo.NotificationsRepository;
import com.foodwaste.repo.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationsService {

    private final NotificationsRepository notificationsRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationsService(
            NotificationsRepository notificationsRepository,
            UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {

        this.notificationsRepository = notificationsRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ----------------------------------------
    // NEW FOOD -> ALL RECEIVERS
    // ----------------------------------------

    public void notifyReceiversAboutNewFood() {

        List<User> receivers =
                userRepository.findByRole(Role.RECEIVER);

        for (User receiver : receivers) {

            Notifications notification = new Notifications();

            notification.setRecipient(receiver);
            notification.setTitle("New Food Posted");
            notification.setMessage(
                    "Someone has posted new food available for claiming."
            );
            notification.setIcon("🍽️");

            Notifications saved =
                    notificationsRepository.save(notification);

            messagingTemplate.convertAndSendToUser(
                    receiver.getEmail(),
                    "/queue/notifications",
                    saved
            );
        }
    }

    // ----------------------------------------
    // FOOD CLAIMED -> ONLY DONOR
    // ----------------------------------------

    public void notifyDonorFoodClaimed(
            User donor,
            User receiver) {

        Notifications notification =
                new Notifications();

        notification.setRecipient(donor);
        notification.setTitle("Food Claimed");
        notification.setMessage(
                "Your food has been claimed by "
                        + receiver.getName()
                        + "."
        );
        notification.setIcon("🍱");

        Notifications saved =
                notificationsRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                donor.getEmail(),
                "/queue/notifications",
                saved
        );
    }

    // ----------------------------------------
    // GET MY NOTIFICATIONS
    // ----------------------------------------

    public List<Notifications> getMyNotifications(
            User user) {

        return notificationsRepository
                .findByRecipientOrderByCreatedAtDesc(user);
    }

    // ----------------------------------------
    // MARK AS READ
    // ----------------------------------------

    public void markAsRead(
            Long id,
            User user) {

        Notifications notification =
                notificationsRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        if (!notification.getRecipient()
                .getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You cannot modify this notification"
            );
        }

        notification.setRead(true);

        notificationsRepository.save(notification);
    }
}