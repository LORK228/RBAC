package ru.mileshko.taxi.trip;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    PassengerDto getPassenger(long passengerId) {
        return userClient.get()
                .uri("/passengers/{id}", passengerId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(PassengerDto.class);
    }

    DriverDto allocateDriver() {
        return userClient.post()
                .uri("/drivers/allocate")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .retrieve()
                .body(DriverDto.class);
    }

    void updateDriverStatus(long driverId, String status) {
        userClient.patch()
                .uri("/drivers/{id}/status", driverId)
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .body(new DriverStatusRequest(status))
                .retrieve()
                .toBodilessEntity();
    }

    void createNotification(NotificationRequest request) {
        notificationClient.post()
                .uri("/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private String bearer() {
        return "Bearer " + jwtService.createToken("trip-service", "SERVICE");
    }

    private record DriverStatusRequest(String status) {
    }
}
