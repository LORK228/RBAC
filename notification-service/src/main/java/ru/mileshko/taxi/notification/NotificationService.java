package ru.mileshko.taxi.notification;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
class NotificationService {
    private final NotificationRepository repository;

    NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    NotificationTask create(CreateNotificationRequest request) {
        return repository.create(request);
    }

    List<NotificationTask> findByTrip(long tripId) {
        return repository.findByTrip(tripId);
    }
}
