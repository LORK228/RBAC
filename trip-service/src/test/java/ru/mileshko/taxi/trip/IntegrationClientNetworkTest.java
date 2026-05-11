package ru.mileshko.taxi.trip;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationClientNetworkTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void getPassenger_connectionRefused_wrappedAsServiceUnavailable() {
        JwtService jwt = new JwtService(SECRET, 60);
        IntegrationClient client = new IntegrationClient(
                RestClient.builder(),
                jwt,
                "http://127.0.0.1:1",
                "http://127.0.0.1:1");

        assertThatThrownBy(() -> client.getPassenger(1L))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("User Service is unavailable")
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void allocateDriver_connectionRefused_wrappedAsServiceUnavailable() {
        JwtService jwt = new JwtService(SECRET, 60);
        IntegrationClient client = new IntegrationClient(
                RestClient.builder(),
                jwt,
                "http://127.0.0.1:1",
                "http://127.0.0.1:1");

        assertThatThrownBy(client::allocateDriver)
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("User Service is unavailable")
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void createNotification_connectionRefused_wrappedAsServiceUnavailable() {
        JwtService jwt = new JwtService(SECRET, 60);
        IntegrationClient client = new IntegrationClient(
                RestClient.builder(),
                jwt,
                "http://127.0.0.1:2",
                "http://127.0.0.1:1");

        NotificationRequest req = new NotificationRequest(1L, "PASSENGER", 10L, "hello");

        assertThatThrownBy(() -> client.createNotification(req))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("Notification Service is unavailable")
                .hasCauseInstanceOf(ResourceAccessException.class);
    }
}
