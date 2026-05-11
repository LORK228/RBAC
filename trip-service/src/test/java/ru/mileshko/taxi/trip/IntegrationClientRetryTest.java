package ru.mileshko.taxi.trip;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringJUnitConfig(IntegrationClientRetryTest.Config.class)
class IntegrationClientRetryTest {

    private static final String USER_BASE = "http://localhost";

    @Configuration
    @EnableRetry
    static class Config {
        private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        @Bean
        JwtService jwtService() {
            return new JwtService(SECRET, 240);
        }

        @Bean
        RestClient.Builder restClientBuilder() {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(800));
            requestFactory.setReadTimeout(Duration.ofMillis(800));
            return RestClient.builder().requestFactory(requestFactory);
        }

        @Bean
        @Lazy
        IntegrationClient integrationClient(RestClient.Builder restClientBuilder, JwtService jwtService) {
            return new IntegrationClient(restClientBuilder, jwtService, USER_BASE, USER_BASE);
        }
    }

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private ApplicationContext applicationContext;

    private MockRestServiceServer server;
    private IntegrationClient integrationClient;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        integrationClient = applicationContext.getBean(IntegrationClient.class);
    }

    @AfterEach
    void tearDown() {
        server.verify();
        server.reset();
    }

    @Test
    void getPassenger_retriesAfterServiceUnavailableThenSucceeds() {
        String body = """
                {"id":10,"name":"Alice","email":"a@a.com","phone":"+70000000001"}
                """;
        server.expect(times(2), requestTo(USER_BASE + "/passengers/10")).andRespond(withServiceUnavailable());
        server.expect(requestTo(USER_BASE + "/passengers/10")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        PassengerDto passenger = integrationClient.getPassenger(10);

        assertThat(passenger.id()).isEqualTo(10);
        assertThat(passenger.name()).isEqualTo("Alice");
    }
}