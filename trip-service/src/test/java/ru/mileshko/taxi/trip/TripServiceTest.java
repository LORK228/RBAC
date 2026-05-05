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
    void createsTripAssignsDriverCalculatesPriceAndQueuesNotifications() {
        when(integrationClient.getPassenger(10L))
                .thenReturn(new PassengerDto(10, "Alice", "alice@example.com", "+70000000001"));
        when(integrationClient.allocateDriver())
                .thenReturn(new DriverDto(20, "Bob", "bob@example.com", "+70000000002", "A123BC", "BUSY"));
        Trip saved = trip(1, 10, 20, TripStatus.DRIVER_ASSIGNED, BigDecimal.valueOf(500), null);
        when(repository.create(eq(10L), eq(20L), eq("A"), eq("B"), eq(BigDecimal.valueOf(12.5)), eq(BigDecimal.valueOf(500.00).setScale(2))))
                .thenReturn(saved);

        Trip result = service.create(new CreateTripRequest(10L, "A", "B", BigDecimal.valueOf(12.5)));

        assertThat(result.id()).isEqualTo(1);
        assertThat(result.price()).isEqualByComparingTo("500");
        ArgumentCaptor<NotificationRequest> notifications = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(integrationClient, times(2)).createNotification(notifications.capture());
        assertThat(notifications.getAllValues())
                .extracting(NotificationRequest::recipientType)
                .containsExactly("PASSENGER", "DRIVER");
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
    void failsWithConflictWhenNoDriversAreAvailable() {
        when(integrationClient.getPassenger(10L))
                .thenReturn(new PassengerDto(10, "Alice", "alice@example.com", "+70000000001"));
        when(integrationClient.allocateDriver())
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "No available drivers"));

        assertThatThrownBy(() -> service.create(new CreateTripRequest(10L, "A", "B", BigDecimal.TEN)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verifyNoInteractions(repository);
    }

    @Test
    void completingTripReleasesDriverAndQueuesStatusNotifications() {
        Trip existing = trip(1, 10, 20, TripStatus.STARTED, BigDecimal.valueOf(500), null);
        Trip completed = trip(1, 10, 20, TripStatus.COMPLETED, BigDecimal.valueOf(500), null);
        when(repository.find(1L)).thenReturn(Optional.of(existing));
        when(repository.updateStatus(1L, TripStatus.COMPLETED)).thenReturn(Optional.of(completed));

        Trip result = service.updateStatus(1L, TripStatus.COMPLETED);

        assertThat(result.status()).isEqualTo(TripStatus.COMPLETED);
        verify(integrationClient).updateDriverStatus(20L, "AVAILABLE");
        verify(integrationClient, times(2)).createNotification(any(NotificationRequest.class));
    }

    @Test
    void ratesOnlyCompletedTrips() {
        Trip rated = trip(1, 10, 20, TripStatus.COMPLETED, BigDecimal.valueOf(500), 5);
        when(repository.rate(1L, 5)).thenReturn(Optional.of(rated));

        assertThat(service.rate(1L, 5).rating()).isEqualTo(5);
    }

    private Trip trip(long id, long passengerId, long driverId, TripStatus status, BigDecimal price, Integer rating) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Trip(id, passengerId, driverId, status, "A", "B", BigDecimal.valueOf(12.5), price, rating, now, now);
    }
}
