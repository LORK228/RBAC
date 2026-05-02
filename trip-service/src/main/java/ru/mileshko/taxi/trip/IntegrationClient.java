package ru.mileshko.taxi.trip;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
class IntegrationClient {
    private final RestClient userClient;
    private final RestClient notificationClient;
    private final JwtService jwtService;

    IntegrationClient(RestClient.Builder builder,
                      JwtService jwtService,
                      @Value("${integrations.user-service-url}") String userServiceUrl,
                      @Value("${integrations.notification-service-url}") String notificationServiceUrl) {
        this.userClient = builder.baseUrl(userServiceUrl).build();
        this.notificationClient = builder.baseUrl(notificationServiceUrl).build();
        this.jwtService = jwtService;
    }

    @Retryable(retryFor = RemoteServiceUnavailableException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 250, multiplier = 2.0))
    PassengerDto getPassenger(long passengerId) {
        return call("User Service", () -> userClient.get()
                .uri("/passengers/{id}", passengerId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(PassengerDto.class));
    }

    @Retryable(retryFor = RemoteServiceUnavailableException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 250, multiplier = 2.0))
    DriverDto allocateDriver() {
        return call("User Service", () -> userClient.post()
                .uri("/drivers/allocate")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(DriverDto.class));
    }

    @Retryable(retryFor = RemoteServiceUnavailableException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 250, multiplier = 2.0))
    void updateDriverStatus(long driverId, String status) {
        call("User Service", () -> userClient.patch()
                .uri("/drivers/{id}/status", driverId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .body(new DriverStatusRequest(status))
                .retrieve()
                .toBodilessEntity());
    }

    @Retryable(retryFor = RemoteServiceUnavailableException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 250, multiplier = 2.0))
    void createNotification(NotificationRequest request) {
        call("Notification Service", () -> notificationClient.post()
                .uri("/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .body(request)
                .retrieve()
                .toBodilessEntity());
    }

    private String bearer() {
        return "Bearer " + jwtService.createToken("trip-service", "SERVICE");
    }

    private <T> T call(String serviceName, RemoteCall<T> call) {
        try {
            return call.execute();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getResponseBodyAsString(), ex);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new RemoteServiceUnavailableException(serviceName + " is unavailable", ex);
        }
    }

    private interface RemoteCall<T> {
        T execute();
    }

    private record DriverStatusRequest(String status) {
    }
}

class RemoteServiceUnavailableException extends ResponseStatusException {
    RemoteServiceUnavailableException(String reason, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, reason, cause);
    }
}
