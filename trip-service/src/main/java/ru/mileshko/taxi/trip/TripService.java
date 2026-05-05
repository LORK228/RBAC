package ru.mileshko.taxi.trip;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
class TripService {
    private final TripRepository repository;
    private final IntegrationClient integrationClient;
    private final TripWebSocketHandler webSocketHandler;
    private final BigDecimal tariffPerKm;

    TripService(TripRepository repository,
                IntegrationClient integrationClient,
                TripWebSocketHandler webSocketHandler,
                @Value("${pricing.tariff-per-km}") BigDecimal tariffPerKm) {
        this.repository = repository;
        this.integrationClient = integrationClient;
        this.webSocketHandler = webSocketHandler;
        this.tariffPerKm = tariffPerKm;
    }

    Trip create(CreateTripRequest request) {
        PassengerDto passenger = integrationClient.getPassenger(request.passengerId());
        DriverDto driver = integrationClient.allocateDriver();
        BigDecimal distance = request.distanceKm() != null
                ? request.distanceKm()
                : estimateDistance(request.origin(), request.destination());
        BigDecimal price = distance.multiply(tariffPerKm).setScale(2, RoundingMode.HALF_UP);

        Trip trip = repository.create(passenger.id(), driver.id(), request.origin(), request.destination(), distance, price);
        webSocketHandler.broadcastStatusChange(trip.id(), trip.status());
        notifyPassenger(trip, "Trip " + trip.id() + " created. Driver " + driver.name() + " assigned.");
        notifyDriver(trip, "Trip " + trip.id() + " assigned from " + trip.origin() + " to " + trip.destination() + ".");
        return trip;
    }

    Trip get(long id) {
        return repository.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
    }

    List<Trip> history(long passengerId) {
        return repository.findByPassenger(passengerId);
    }

    Trip updateStatus(long id, TripStatus status) {
        Trip before = get(id);
        Trip updated = repository.updateStatus(id, status)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        webSocketHandler.broadcastStatusChange(id, status);

        if (status == TripStatus.COMPLETED || status == TripStatus.CANCELLED) {
            integrationClient.updateDriverStatus(before.driverId(), "AVAILABLE");
        }

        notifyPassenger(updated, "Trip " + id + " status changed to " + status + ".");
        notifyDriver(updated, "Trip " + id + " status changed to " + status + ".");
        return updated;
    }

    Trip rate(long id, int rating) {
        return repository.rate(id, rating)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Only completed trips can be rated"));
    }

    TripStatistics statistics(LocalDate date) {
        return repository.statistics(date);
    }

    private void notifyPassenger(Trip trip, String message) {
        integrationClient.createNotification(new NotificationRequest(trip.id(), "PASSENGER", trip.passengerId(), message));
    }

    private void notifyDriver(Trip trip, String message) {
        if (trip.driverId() != null) {
            integrationClient.createNotification(new NotificationRequest(trip.id(), "DRIVER", trip.driverId(), message));
        }
    }

    private BigDecimal estimateDistance(String origin, String destination) {
        int hash = Math.abs((origin + destination).hashCode());
        return BigDecimal.valueOf(3 + (hash % 250) / 10.0).setScale(2, RoundingMode.HALF_UP);
    }
}
