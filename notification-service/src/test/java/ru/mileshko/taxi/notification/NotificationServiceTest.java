package ru.mileshko.taxi.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService service;

    @Test
    void create_delegatesToRepository() {
        CreateNotificationRequest req = new CreateNotificationRequest(1L, "PASSENGER", 10L, "msg");
        OffsetDateTime now = OffsetDateTime.now();
        NotificationTask task = new NotificationTask(1, 1, RecipientType.PASSENGER, 10, "msg",
                NotificationStatus.PENDING, 0, now, now);
        when(repository.create(req)).thenReturn(task);

        assertThat(service.create(req)).isEqualTo(task);
    }

    @Test
    void findByTrip_delegatesToRepository() {
        when(repository.findByTrip(5L)).thenReturn(List.of());

        assertThat(service.findByTrip(5L)).isEmpty();
        verify(repository).findByTrip(5L);
    }

    @Test
    void findByRecipient_delegatesToRepository() {
        when(repository.findByRecipient("DRIVER", 9L)).thenReturn(List.of());

        assertThat(service.findByRecipient("DRIVER", 9L)).isEmpty();
    }
}
