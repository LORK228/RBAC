package ru.mileshko.taxi.notification;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtService jwtService;

    private final OffsetDateTime now = OffsetDateTime.now();

    @Test
    void create_returnsTask() throws Exception {
        NotificationTask task = new NotificationTask(1, 10, RecipientType.PASSENGER, 20, "hi",
                NotificationStatus.PENDING, 0, now, now);
        when(notificationService.create(any(CreateNotificationRequest.class))).thenReturn(task);

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":10,\"recipientType\":\"PASSENGER\",\"recipientId\":20,\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void findByTrip_returnsList() throws Exception {
        NotificationTask task = new NotificationTask(1, 10, RecipientType.DRIVER, 3, "m",
                NotificationStatus.SENT, 1, now, now);
        when(notificationService.findByTrip(10L)).thenReturn(List.of(task));

        mockMvc.perform(get("/notifications").param("trip_id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tripId").value(10));
    }

    @Test
    void findByRecipient_returnsList() throws Exception {
        when(notificationService.findByRecipient(eq("PASSENGER"), eq(99L))).thenReturn(List.of());

        mockMvc.perform(get("/notifications")
                        .param("recipient_type", "PASSENGER")
                        .param("recipient_id", "99"))
                .andExpect(status().isOk());
    }
}
