package ru.mileshko.taxi.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

record Passenger(long id, String name, String email, String phone, OffsetDateTime createdAt) {
}

record Driver(long id, String name, String email, String phone, String licenseNumber, DriverStatus status,
              OffsetDateTime createdAt) {
}

enum DriverStatus {
    AVAILABLE, BUSY, OFFLINE
}

record CreatePassengerRequest(@NotBlank String name, @Email String email, @NotBlank String phone) {
}

record CreateDriverRequest(@NotBlank String name, @Email String email, @NotBlank String phone,
                           @NotBlank String licenseNumber) {
}

record UpdateDriverStatusRequest(@Pattern(regexp = "AVAILABLE|BUSY|OFFLINE") String status) {
}

record TokenRequest(@NotBlank String subject, @NotBlank String role) {
}

record TokenResponse(String token) {
}
