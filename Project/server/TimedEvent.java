package Project.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

public class TimedEvent {
    private long time;
    private Callable<Boolean> runEvent; // the event to be run in the timeframe
    private Runnable failEvent; // the event to be run if the time is exceeded
    private ExecutorService executor;

    public TimedEvent(Callable<Boolean> runEvent, Runnable failEvent, long time) {
        this.time = time;
        this.runEvent = runEvent;
        this.failEvent = failEvent;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void start() {
        Future<Boolean> future = executor.submit(runEvent);
        try {
            if (!future.get(time, TimeUnit.SECONDS)) {
                failEvent.run();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            future.cancel(true);
            failEvent.run();
        } finally {
            executor.shutdown();
        }
    }
}