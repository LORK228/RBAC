package ru.mileshko.taxi.notification;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWorkerPoolTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);

    @Test
    void workerClaimsAndSendsPendingTask() throws Exception {
        NotificationTask task = task(1, 1);
        when(repository.claimNext()).thenReturn(Optional.of(task), Optional.empty());
        NotificationWorkerPool pool = new NotificationWorkerPool(repository, 1, 50);

        pool.start();
        verify(repository, timeout(1000)).markSent(1L);
        pool.shutdown();
    }

    @Test
    void workerReturnsTaskToRetryWhenSendingFails() throws Exception {
        NotificationTask task = task(2, 1);
        when(repository.claimNext()).thenReturn(Optional.of(task), Optional.empty());
        doThrow(new RuntimeException("network write failed")).when(repository).markSent(2L);
        NotificationWorkerPool pool = new NotificationWorkerPool(repository, 1, 50);

        pool.start();
        verify(repository, timeout(1000)).markFailedOrRetry(2L, 1);
        pool.shutdown();
    }

    private NotificationTask task(long id, int attempts) {
        OffsetDateTime now = OffsetDateTime.now();
        return new NotificationTask(id, 100, RecipientType.PASSENGER, 200, "message",
                NotificationStatus.PROCESSING, attempts, now, now);
    }
}
