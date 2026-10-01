package com.foodwaste.controller;

import com.foodwaste.dto.Dtos.*;
import com.foodwaste.service.ListingEvents;
import com.foodwaste.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ListingController {
    private final ListingService service;
    private final ListingEvents events;

    public ListingController(ListingService service, ListingEvents events) {
        this.service = service;
        this.events = events;
    }

    @GetMapping("/listings")
    public List<ListingResponse> all() {
        return service.all();
    }

    /** Available listings within the fixed 10 km radius of the receiver's current location. */
    @GetMapping("/listings/nearby")
    public List<ListingResponse> nearby(@RequestParam double latitude, @RequestParam double longitude) {
        return service.nearby(latitude, longitude);
    }

    @GetMapping("/listings/expiring-soon")
    public List<ListingResponse> expiringSoon() {
        return service.expiringSoon();
    }

    /** The logged-in donor's own posted listings, for the donor dashboard. */
    @GetMapping("/listings/mine")
    @PreAuthorize("hasRole('DONOR')")
    public List<ListingResponse> mine(Authentication auth) {
        return service.mine(auth.getName());
    }

    @PostMapping("/listings")
    @PreAuthorize("hasRole('DONOR')")
    public ListingResponse create(@Valid @RequestBody ListingRequest r, Authentication auth) {
        ListingResponse l = service.create(r, auth.getName());
        events.publish("NEW", l);
        return l;
    }

    @PostMapping("/listings/{id}/claim")
    @PreAuthorize("hasRole('RECEIVER')")
    public ListingResponse claim(@PathVariable Long id, Authentication auth) {
        ListingResponse l = service.claim(id, auth.getName());
        events.publish("CLAIMED", l);
        return l;
    }

    @PostMapping("/listings/{id}/cancel")
    @PreAuthorize("hasRole('RECEIVER')")
    public ListingResponse cancel(@PathVariable Long id, Authentication auth) {
        ListingResponse l = service.cancelClaim(id, auth.getName());
        events.publish("CANCELLED", l);
        return l;
    }

    @PostMapping("/listings/{id}/pickup")
    @PreAuthorize("hasRole('DONOR')")
    public ListingResponse pickup(@PathVariable Long id, Authentication auth) {
        ListingResponse l = service.markPickedUp(id, auth.getName());
        events.publish("PICKED_UP", l);
        return l;
    }

    @DeleteMapping("/listings/{id}")
    @PreAuthorize("hasAnyRole('DONOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        ListingResponse l = service.delete(id, auth.getName(), admin);
        events.publish("DELETED", l);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listings/{id}/contact")
    public ContactResponse contact(@PathVariable Long id, Authentication auth) {
        return service.contact(id, auth.getName());
    }

    @GetMapping("/stats")
    public Stats stats() {
        return service.stats();
    }

    @GetMapping("/donor/stats")
    @PreAuthorize("hasRole('DONOR')")
    public DonorStats donorStats(Authentication auth) {
        return service.donorStats(auth.getName());
    }
}
