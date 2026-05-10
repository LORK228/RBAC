package ru.mileshko.taxi.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void token_returnsJwtFromService() throws Exception {
        when(jwtService.createToken("trip", "SERVICE")).thenReturn("signed-jwt");

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRequest("trip", "SERVICE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed-jwt"));
    }

    @Test
    void createPassenger_returnsBody() throws Exception {
        Passenger p = new Passenger(1, "N", "n@e.com", "+1", now);
        when(userService.createPassenger(any(CreatePassengerRequest.class))).thenReturn(p);

        mockMvc.perform(post("/passengers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"n@e.com\",\"phone\":\"+1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getPassenger_returnsBody() throws Exception {
        Passenger p = new Passenger(2, "N", "n@e.com", "+1", now);
        when(userService.getPassenger(2L)).thenReturn(p);

        mockMvc.perform(get("/passengers/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("n@e.com"));
    }

    @Test
    void createDriver_returnsBody() throws Exception {
        Driver d = new Driver(3, "D", "d@e.com", "+1", "LIC", DriverStatus.AVAILABLE, now);
        when(userService.createDriver(any(CreateDriverRequest.class))).thenReturn(d);

        mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"D\",\"email\":\"d@e.com\",\"phone\":\"+1\",\"licenseNumber\":\"LIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void availableDrivers_returnsList() throws Exception {
        Driver d = new Driver(1, "D", "d@e.com", "+1", "L", DriverStatus.AVAILABLE, now);
        when(userService.availableDrivers()).thenReturn(List.of(d));

        mockMvc.perform(get("/drivers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void allocateDriver_returnsBody() throws Exception {
        Driver d = new Driver(4, "D", "d@e.com", "+1", "L", DriverStatus.BUSY, now);
        when(userService.allocateDriver()).thenReturn(d);

        mockMvc.perform(post("/drivers/allocate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BUSY"));
    }

    @Test
    void patchDriverStatus_returnsBody() throws Exception {
        Driver d = new Driver(4, "D", "d@e.com", "+1", "L", DriverStatus.OFFLINE, now);
        when(userService.updateStatus(eq(4L), eq(DriverStatus.OFFLINE))).thenReturn(d);

        mockMvc.perform(patch("/drivers/4/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFLINE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFFLINE"));
    }
}
