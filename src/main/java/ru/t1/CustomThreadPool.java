import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CustomThreadPool {
    private final int capacity;                    // Количество рабочих потоков
    private final List<Worker> workers;            // Список рабочих потоков
    private final Queue<Runnable> taskQueue;       // Очередь задач
    private boolean isShutdown = false;            // Флаг завершения пула

    // Конструктор пула с указанной емкостью
    public CustomThreadPool(int capacity) {
        this.capacity = capacity;
        this.taskQueue = new LinkedList<>();
        this.workers = new LinkedList<>();

        // Инициализация и запуск потоков
        for (int i = 0; i < capacity; i++) {
            Worker worker = new Worker();
            workers.add(worker);
            worker.start();
        }
    }

    // Метод для добавления задачи в пул
    public synchronized void execute(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("ThreadPool отключен.");
        }
        // Добавление задачи в очередь и уведомление потоков
        taskQueue.offer(task);
        notify();
    }

    // Метод для завершения работы пула
    public synchronized void shutdown() {
        isShutdown = true;
        // Прерывание всех ожидающих потоков
        for (Worker worker : workers) {
            worker.interrupt();
        }
    }

    // Метод для ожидания завершения всех задач после shutdown
    public synchronized void awaitTermination() {
        for (Worker worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Внутренний класс Worker - поток, выполняющий задачи из очереди
    private class Worker extends Thread {
        @Override
        public void run() {
            while (true) {
                Runnable task;
                synchronized (CustomThreadPool.this) {
                    // Ожидание задачи в очереди
                    while (taskQueue.isEmpty()) {
                        if (isShutdown) {
                            return; // Завершение работы потока, если пул остановлен и задач больше нет
                        }
                        try {
                            CustomThreadPool.this.wait();
                        } catch (InterruptedException e) {
                            if (isShutdown) {
                                return;
                            }
                            Thread.currentThread().interrupt();
                        }
                    }
                    // Извлечение задачи из очереди
                    task = taskQueue.poll();
                }
                // Выполнение задачи вне блока synchronized
                if (task != null) {
                    try {
                        task.run();
                    } catch (RuntimeException e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                }
            }
        }
    }
}
