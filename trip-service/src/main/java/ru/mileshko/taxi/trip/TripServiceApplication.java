package ru.mileshko.taxi.trip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@EnableRetry
@SpringBootApplication
public class TripServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }

    @Bean
    RestClient.Builder restClientBuilder(@Value("${integrations.connect-timeout-ms}") int connectTimeoutMs,
                                         @Value("${integrations.read-timeout-ms}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
