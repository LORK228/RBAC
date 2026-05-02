package ru.mileshko.taxi.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

record NotificationTask(long id, long tripId, RecipientType recipientType, long recipientId, String message,
                        NotificationStatus status, int attempts, OffsetDateTime createdAt,
                        OffsetDateTime updatedAt) {
}

enum RecipientType {
    PASSENGER, DRIVER
}

enum NotificationStatus {
    PENDING, PROCESSING, SENT, FAILED
}

record CreateNotificationRequest(@NotNull Long tripId,
                                 @Pattern(regexp = "PASSENGER|DRIVER") String recipientType,
                                 @NotNull Long recipientId,
                                 @NotBlank String message) {
}
