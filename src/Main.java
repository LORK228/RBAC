public class Main {

    private static final int THREAD_COUNT = 5;
    private static final int WORK_LENGTH = 30;
    private static final int DELAY = 100;

    private static final Object LOCK = new Object();

    static class MyTask implements Runnable {
        private final int index;

        public MyTask(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            long id = Thread.currentThread().getId();
            long startTime = System.currentTimeMillis();

            for (int i = 0; i <= WORK_LENGTH; i++) {

                draw(index, id, i, -1);

                try {
                    Thread.sleep(DELAY + (int)(Math.random() * 100));
                } catch (InterruptedException e) {
                    return;
                }
            }

            long time = System.currentTimeMillis() - startTime;

            draw(index, id, WORK_LENGTH, time);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        Thread[] threads = new Thread[THREAD_COUNT];

        clearScreen();

        System.out.println("Многопоточный прогресс:");
        System.out.println();

        // резервируем строки под потоки
        for (int i = 0; i < THREAD_COUNT; i++) {
            System.out.println();
        }

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(new MyTask(i));
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        moveCursor(THREAD_COUNT + 3, 1);
        System.out.println("\nВсе потоки завершены.");
    }

    private static void draw(int index, long id, int progress, long time) {

        int percent = progress * 100 / WORK_LENGTH;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < WORK_LENGTH; i++) {
            if (i < progress) bar.append("#");
            else bar.append("-");
        }

        String line = String.format(
                "Поток #%d | id=%d | [%s] %3d%%",
                index + 1, id, bar, percent
        );

        if (time >= 0) {
            line += String.format(" | время: %d мс", time);
        }

        synchronized (LOCK) {
            moveCursor(index + 3, 1);
            System.out.print("\033[2K"); 
            System.out.print(line);
            System.out.flush();
        }
    }

    private static void moveCursor(int row, int col) {
        System.out.print("\033[" + row + ";" + col + "H");
    }

    private static void clearScreen() {
        System.out.print("\033[2J");
        System.out.print("\033[H");
        System.out.flush();
    }
}
