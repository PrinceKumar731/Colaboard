package com.codejudge.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Fair-scheduling queue: submissions enter a LinkedBlockingQueue and are
 * consumed by a fixed thread pool of worker goroutines.  Each worker calls
 * SubmissionProcessor.process() which runs code in Docker and persists results.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionQueueService {

    private final SubmissionProcessor submissionProcessor;

    @Value("${judge.queue.workers:3}")
    private int workerCount;

    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private ExecutorService workerPool;
    private volatile boolean running = false;

    @EventListener(ApplicationReadyEvent.class)
    public void startWorkers() {
        running = true;
        workerPool = Executors.newFixedThreadPool(workerCount,
                r -> {
                    Thread t = new Thread(r, "judge-worker-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                });
        for (int i = 0; i < workerCount; i++) {
            workerPool.submit(this::workerLoop);
        }
        log.info("Execution queue started with {} workers", workerCount);
    }

    public void enqueue(String submissionId) {
        queue.offer(submissionId);
        log.info("Queued submission {} (depth: {})", submissionId, queue.size());
    }

    public int getQueueDepth() {
        return queue.size();
    }

    private void workerLoop() {
        while (running) {
            try {
                String submissionId = queue.poll(1, TimeUnit.SECONDS);
                if (submissionId != null) {
                    log.info("Worker picked up submission {}", submissionId);
                    submissionProcessor.process(submissionId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker error: {}", e.getMessage(), e);
            }
        }
        log.info("Worker {} shutting down", Thread.currentThread().getName());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down execution queue…");
        running = false;
        if (workerPool != null) {
            workerPool.shutdown();
            try {
                if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    workerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
