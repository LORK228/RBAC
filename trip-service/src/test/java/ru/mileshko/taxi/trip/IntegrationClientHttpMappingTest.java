package ru.mileshko.taxi.trip;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IntegrationClientHttpMappingTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String USER_BASE = "http://localhost";

    @Test
    void getPassenger_notFound_mapsToResponseStatusNotFound() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JwtService jwt = new JwtService(SECRET, 60);
        IntegrationClient client = new IntegrationClient(builder, jwt, USER_BASE, USER_BASE);

        server.expect(requestTo(USER_BASE + "/passengers/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getPassenger(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");

        server.verify();
    }

    @Test
    void allocateDriver_conflict_mapsToResponseStatusConflict() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JwtService jwt = new JwtService(SECRET, 60);
        IntegrationClient client = new IntegrationClient(builder, jwt, USER_BASE, USER_BASE);

        server.expect(requestTo(USER_BASE + "/drivers/allocate"))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("No available drivers"));

        assertThatThrownBy(client::allocateDriver)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        server.verify();
    }

    @Test
    void singleServiceUnavailable_mapsToRemoteServiceUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JwtService jwt = new JwtService(SECRET, 60);
        IntegrationClient client = new IntegrationClient(builder, jwt, USER_BASE, USER_BASE);

        server.expect(requestTo(USER_BASE + "/passengers/1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getPassenger(1L))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("User Service is unavailable");

        server.verify();
    }

    @Test
    void getPassenger_success_returnsDto() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JwtService jwt = new JwtService(SECRET, 60);
        IntegrationClient client = new IntegrationClient(builder, jwt, USER_BASE, USER_BASE);

        String json = """
                {"id":10,"name":"Alice","email":"a@a.com","phone":"+70000000001"}
                """;
        server.expect(requestTo(USER_BASE + "/passengers/10"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        PassengerDto dto = client.getPassenger(10L);
        org.assertj.core.api.Assertions.assertThat(dto.id()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(dto.name()).isEqualTo("Alice");

        server.verify();
    }
}
