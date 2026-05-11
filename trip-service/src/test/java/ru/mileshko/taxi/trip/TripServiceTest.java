package ru.mileshko.taxi.trip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {
    private final TripRepository repository = mock(TripRepository.class);
    private final IntegrationClient integrationClient = mock(IntegrationClient.class);
    private final TripWebSocketHandler webSocketHandler = mock(TripWebSocketHandler.class);
    private final TripService service = new TripService(repository, integrationClient, webSocketHandler, BigDecimal.valueOf(40));

    @Test
    void createsTripWithCreatedStatusAndQueuesPassengerNotification() {
        when(integrationClient.getPassenger(10L))
                .thenReturn(new PassengerDto(10, "Alice", "alice@example.com", "+70000000001"));
        Trip saved = trip(1, 10, null, TripStatus.CREATED, BigDecimal.valueOf(500), null);
        when(repository.create(eq(10L), eq("A"), eq("B"), eq(BigDecimal.valueOf(12.5)), eq(BigDecimal.valueOf(500.00).setScale(2))))
                .thenReturn(saved);

        Trip result = service.create(new CreateTripRequest(10L, "A", "B", BigDecimal.valueOf(12.5)));

        assertThat(result.id()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(TripStatus.CREATED);
        assertThat(result.driverId()).isNull();
        assertThat(result.price()).isEqualByComparingTo("500");
        ArgumentCaptor<NotificationRequest> notifications = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(integrationClient).createNotification(notifications.capture());
        assertThat(notifications.getValue().recipientType()).isEqualTo("PASSENGER");
    }

    @Test
    void failsWithServiceUnavailableWhenUserServiceIsDown() {
        when(integrationClient.getPassenger(10L))
                .thenThrow(new RemoteServiceUnavailableException("User Service is unavailable", new RuntimeException()));

        assertThatThrownBy(() -> service.create(new CreateTripRequest(10L, "A", "B", BigDecimal.TEN)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503 SERVICE_UNAVAILABLE");
        verifyNoInteractions(repository);
    }

    @Test
    void assignsDriverToCreatedTrip() {
        Trip created = trip(1, 10, null, TripStatus.CREATED, BigDecimal.valueOf(500), null);
        Trip assigned = trip(1, 10, 20L, TripStatus.DRIVER_ASSIGNED, BigDecimal.valueOf(500), null);
        when(repository.find(1L)).thenReturn(Optional.of(created));
        when(repository.assignDriver(1L, 20L)).thenReturn(Optional.of(assigned));

        Trip result = service.assignDriver(1L, 20L);

        assertThat(result.status()).isEqualTo(TripStatus.DRIVER_ASSIGNED);
        assertThat(result.driverId()).isEqualTo(20L);
        verify(integrationClient, times(2)).createNotification(any(NotificationRequest.class));
    }

    @Test
    void failsAssigningDriverToNonCreatedTrip() {
        Trip started = trip(1, 10, 20L, TripStatus.STARTED, BigDecimal.valueOf(500), null);
        when(repository.find(1L)).thenReturn(Optional.of(started));

        assertThatThrownBy(() -> service.assignDriver(1L, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verifyNoInteractions(integrationClient);
    }

    @Test
    void completingTripReleasesDriverAndQueuesStatusNotifications() {
        Trip existing = trip(1, 10, 20L, TripStatus.STARTED, BigDecimal.valueOf(500), null);
        Trip completed = trip(1, 10, 20L, TripStatus.COMPLETED, BigDecimal.valueOf(500), null);
        when(repository.find(1L)).thenReturn(Optional.of(existing));
        when(repository.updateStatus(1L, TripStatus.COMPLETED)).thenReturn(Optional.of(completed));

        Trip result = service.updateStatus(1L, TripStatus.COMPLETED);

        assertThat(result.status()).isEqualTo(TripStatus.COMPLETED);
        verify(integrationClient).updateDriverStatus(20L, "AVAILABLE");
        verify(integrationClient, times(2)).createNotification(any(NotificationRequest.class));
    }

    @Test
    void ratesOnlyCompletedTrips() {
        Trip rated = trip(1, 10, 20L, TripStatus.COMPLETED, BigDecimal.valueOf(500), 5);
        when(repository.rate(1L, 5)).thenReturn(Optional.of(rated));

        assertThat(service.rate(1L, 5).rating()).isEqualTo(5);
    }

    private Trip trip(long id, long passengerId, Long driverId, TripStatus status, BigDecimal price, Integer rating) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Trip(id, passengerId, driverId, status, "A", "B", BigDecimal.valueOf(12.5), price, rating, now, now);
    }
}
