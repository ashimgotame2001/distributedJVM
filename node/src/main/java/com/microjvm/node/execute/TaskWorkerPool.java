package com.microjvm.node.execute;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class TaskWorkerPool implements AutoCloseable {
  private final ExecutorService executor;

  public TaskWorkerPool(int threads) {
    AtomicInteger seq = new AtomicInteger();
    ThreadFactory factory =
        r -> {
          Thread t = new Thread(r, "microjvm-worker-" + seq.incrementAndGet());
          t.setDaemon(true);
          return t;
        };
    this.executor = Executors.newFixedThreadPool(Math.max(1, threads), factory);
  }

  public void submit(Runnable task) {
    executor.execute(task);
  }

  @Override
  public void close() {
    executor.shutdownNow();
  }
}
