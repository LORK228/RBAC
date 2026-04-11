import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BackgroundExecutor implements AutoCloseable {
    private final ExecutorService executorService;

    public BackgroundExecutor(int threads) {
        int poolSize = Math.max(2, threads);
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public Future<?> submit(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        return executorService.submit(task);
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}
