package ru.mileshko.taxi.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = TripController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripService tripService;

    @MockBean
    private JwtService jwtService;

    @Test
    void createTrip_returnsBody() throws Exception {
        Trip trip = new Trip(1, 10, 20L, TripStatus.DRIVER_ASSIGNED, "A", "B",
                BigDecimal.TEN, BigDecimal.valueOf(400), null, OffsetDateTime.now(), OffsetDateTime.now());
        when(tripService.create(any(CreateTripRequest.class))).thenReturn(trip);

        mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTripRequest(10L, "A", "B", BigDecimal.TEN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRIVER_ASSIGNED"));
    }

    @Test
    void getTrip_returnsBody() throws Exception {
        Trip trip = new Trip(2, 10, 20L, TripStatus.STARTED, "A", "B",
                BigDecimal.TEN, BigDecimal.valueOf(400), null, OffsetDateTime.now(), OffsetDateTime.now());
        when(tripService.get(2L)).thenReturn(trip);

        mockMvc.perform(get("/trips/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void history_returnsList() throws Exception {
        Trip trip = new Trip(1, 10, null, TripStatus.COMPLETED, "A", "B",
                BigDecimal.TEN, BigDecimal.ONE, 5, OffsetDateTime.now(), OffsetDateTime.now());
        when(tripService.history(10L)).thenReturn(List.of(trip));

        mockMvc.perform(get("/trips").param("passenger_id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void updateStatus_returnsBody() throws Exception {
        Trip trip = new Trip(1, 10, 20L, TripStatus.COMPLETED, "A", "B",
                BigDecimal.TEN, BigDecimal.valueOf(400), null, OffsetDateTime.now(), OffsetDateTime.now());
        when(tripService.updateStatus(eq(1L), eq(TripStatus.COMPLETED))).thenReturn(trip);

        mockMvc.perform(patch("/trips/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void rate_returnsBody() throws Exception {
        Trip trip = new Trip(1, 10, 20L, TripStatus.COMPLETED, "A", "B",
                BigDecimal.TEN, BigDecimal.valueOf(400), 4, OffsetDateTime.now(), OffsetDateTime.now());
        when(tripService.rate(1L, 4)).thenReturn(trip);

        mockMvc.perform(patch("/trips/1/rating")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void statistics_returnsBody() throws Exception {
        LocalDate day = LocalDate.of(2026, 5, 10);
        when(tripService.statistics(day)).thenReturn(new TripStatistics(day, 5, BigDecimal.valueOf(99.5)));

        mockMvc.perform(get("/trips/statistics").param("date", "2026-05-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripsCount").value(5));
    }
}
