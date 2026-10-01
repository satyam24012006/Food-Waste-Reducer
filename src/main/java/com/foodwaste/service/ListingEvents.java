package com.foodwaste.service;

import com.foodwaste.dto.Dtos.ListingEvent;
import com.foodwaste.dto.Dtos.ListingResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Pushes listing changes after the database transaction has committed. */
@Component
public class ListingEvents {
    public static final String TOPIC = "/topic/listings";
    private final SimpMessagingTemplate template;

    public ListingEvents(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void publish(String type, ListingResponse listing) {
        Runnable send = () -> template.convertAndSend(TOPIC, new ListingEvent(type, listing));

        // Important: claim/pickup changes are broadcast only AFTER the DB commit.
        // Otherwise the other browser can refresh immediately and still read the old status.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            send.run();
                        }
                    });
        } else {
            send.run();
        }
    }
}
