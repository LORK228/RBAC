package ru.mileshko.taxi.notification;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class NotificationController {
    private final NotificationService service;

    NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/notifications")
    NotificationTask create(@Valid @RequestBody CreateNotificationRequest request) {
        return service.create(request);
    }

    @GetMapping(value = "/notifications", params = "trip_id")
    List<NotificationTask> findByTrip(@RequestParam("trip_id") long tripId) {
        return service.findByTrip(tripId);
    }
}
