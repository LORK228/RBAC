package ru.mileshko.taxi.trip;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

record Trip(long id, long passengerId, Long driverId, TripStatus status, String origin, String destination,
            BigDecimal distanceKm, BigDecimal price, Integer rating, OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
}

enum TripStatus {
    CREATED, DRIVER_ASSIGNED, ACCEPTED, STARTED, COMPLETED, CANCELLED
}

record CreateTripRequest(@NotNull Long passengerId, @NotBlank String origin, @NotBlank String destination,
                         @DecimalMin("0.1") BigDecimal distanceKm) {
}

record UpdateTripStatusRequest(@Pattern(regexp = "ACCEPTED|STARTED|COMPLETED|CANCELLED") String status) {
}

record AssignDriverRequest(@NotNull Long driverId) {
}

record RateTripRequest(@Min(1) @Max(5) int rating) {
}

record TripStatistics(LocalDate date, long tripsCount, BigDecimal averagePrice) {
}

record PassengerDto(long id, String name, String email, String phone) {
}

record DriverDto(long id, String name, String email, String phone, String licenseNumber, String status) {
}

record NotificationRequest(long tripId, String recipientType, long recipientId, String message) {
}
