package ru.mileshko.taxi.notification;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
class NotificationWorkerPool {
    private static final Logger log = LoggerFactory.getLogger(NotificationWorkerPool.class);

    private final NotificationRepository repository;
    private final int poolSize;
    private final long idleSleepMs;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ExecutorService executor;

    NotificationWorkerPool(NotificationRepository repository,
                           @Value("${workers.pool-size}") int poolSize,
                           @Value("${workers.idle-sleep-ms}") long idleSleepMs) {
        this.repository = repository;
        this.poolSize = poolSize;
        this.idleSleepMs = idleSleepMs;
    }

    @PostConstruct
    void start() {
        executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < poolSize; i++) {
            int workerId = i + 1;
            executor.submit(() -> work(workerId));
        }
        log.info("Notification worker pool started with {} workers", poolSize);
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        running.set(false);
        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
        log.info("Notification worker pool stopped");
    }

    private void work(int workerId) {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            Optional<NotificationTask> task = repository.claimNext();
            if (task.isEmpty()) {
                sleep(Duration.ofMillis(idleSleepMs));
                continue;
            }
            process(workerId, task.get());
        }
    }

    private void process(int workerId, NotificationTask task) {
        try {
            log.info("Worker {} sends notification {} to {} {}: {}",
                    workerId, task.id(), task.recipientType(), task.recipientId(), task.message());
            sleep(Duration.ofMillis(300));
            repository.markSent(task.id());
        } catch (RuntimeException ex) {
            log.warn("Worker {} failed notification {} attempt {}", workerId, task.id(), task.attempts(), ex);
            repository.markFailedOrRetry(task.id(), task.attempts());
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
