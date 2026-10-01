package com.foodwaste.scheduler;

import com.foodwaste.service.ListingEvents;
import com.foodwaste.service.ListingService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiryScheduler {
    private final ListingService service;
    private final ListingEvents events;

    public ExpiryScheduler(ListingService service, ListingEvents events) {
        this.service = service;
        this.events = events;
    }

    /** Every minute: expire listings whose best-before time has passed and tell everyone live. */
    @Scheduled(fixedRate = 60_000)
    public void expire() {
        try {
            service.expireOverdue().forEach(l -> events.publish("EXPIRED", l));
        } catch (OptimisticLockingFailureException e) {
            // someone acted on it at the same moment; next run will retry
        }
    }
}
