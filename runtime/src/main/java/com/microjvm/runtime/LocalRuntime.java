package com.microjvm.runtime;

import com.microjvm.common.TaskDescriptor;
import com.microjvm.common.TaskStatus;
import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.common.codec.TaskCodec;
import com.microjvm.proto.TaskEvent;
import com.microjvm.sdk.MicroFuture;
import com.microjvm.sdk.MicroTask;
import com.microjvm.sdk.TaskExecutionException;
import com.microjvm.sdk.TaskSubmitter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Submits tasks into the store and, when placement/dispatch are configured, places them on a fixed
 * node and completes the caller's {@link MicroFuture} from the gRPC event stream.
 */
public final class LocalRuntime implements TaskSubmitter, AutoCloseable {
  private final TaskStore store;
  private final TaskCodec codec;
  private final FixedPlacement placement;
  private final NodeDispatchClient dispatchClient;
  private final ExecutorService dispatchExecutor;
  private final boolean ownsExecutor;

  public LocalRuntime() {
    this(new InMemoryTaskStore(), new JavaSerializationCodec(), null, null, null, false);
  }

  public LocalRuntime(TaskStore store, TaskCodec codec) {
    this(store, codec, null, null, null, false);
  }

  public LocalRuntime(
      TaskStore store,
      TaskCodec codec,
      FixedPlacement placement,
      NodeDispatchClient dispatchClient) {
    this(
        store,
        codec,
        placement,
        dispatchClient,
        Executors.newCachedThreadPool(
            r -> {
              Thread t = new Thread(r, "microjvm-dispatch");
              t.setDaemon(true);
              return t;
            }),
        true);
  }

  LocalRuntime(
      TaskStore store,
      TaskCodec codec,
      FixedPlacement placement,
      NodeDispatchClient dispatchClient,
      ExecutorService dispatchExecutor,
      boolean ownsExecutor) {
    this.store = Objects.requireNonNull(store, "store");
    this.codec = Objects.requireNonNull(codec, "codec");
    this.placement = placement;
    this.dispatchClient = dispatchClient;
    this.dispatchExecutor = dispatchExecutor;
    this.ownsExecutor = ownsExecutor;
    if ((placement == null) != (dispatchClient == null)) {
      throw new IllegalArgumentException("placement and dispatchClient must both be set or both null");
    }
  }

  public TaskStore store() {
    return store;
  }

  public TaskCodec codec() {
    return codec;
  }

  @Override
  public <T> MicroFuture<T> submit(MicroTask<T> task) throws IOException {
    TaskDescriptor descriptor = task.toDescriptor(codec);
    return submit(descriptor, task.resultType());
  }

  @Override
  public <T> MicroFuture<T> submit(TaskDescriptor descriptor, Class<T> resultType) {
    store.put(new StoredTask(descriptor, TaskStatus.CREATED));
    store.updateStatus(descriptor.taskId(), TaskStatus.QUEUED);
    MicroFuture<T> future = new MicroFuture<>(descriptor.taskId());
    if (placement != null) {
      dispatchExecutor.execute(() -> placeAndDispatch(descriptor, resultType, future));
    }
    return future;
  }

  private <T> void placeAndDispatch(
      TaskDescriptor descriptor, Class<T> resultType, MicroFuture<T> future) {
    try {
      NodeTarget target = placement.place(descriptor.taskId());
      StoredTask placed =
          store
              .get(descriptor.taskId())
              .orElseThrow()
              .withPlacement(target, TaskStatus.PLACED);
      store.update(descriptor.taskId(), placed);

      Iterator<TaskEvent> events = dispatchClient.execute(descriptor);
      boolean sawTerminal = false;
      while (events.hasNext()) {
        TaskEvent event = events.next();
        if (event.hasProgress() && placed.status() == TaskStatus.PLACED) {
          store.updateStatus(descriptor.taskId(), TaskStatus.RUNNING);
        }
        if (event.hasResult()) {
          store.updateStatus(descriptor.taskId(), TaskStatus.RUNNING);
          store.updateStatus(descriptor.taskId(), TaskStatus.COMPLETED);
          T value = decodeResult(event.getResult().getPayload().toByteArray(), resultType);
          future.complete(value);
          sawTerminal = true;
          break;
        }
        if (event.hasError()) {
          store.updateStatus(descriptor.taskId(), TaskStatus.FAILED);
          future.completeExceptionally(
              new TaskExecutionException(
                  event.getError().getType(), event.getError().getMessage()));
          sawTerminal = true;
          break;
        }
      }
      if (!sawTerminal) {
        store.updateStatus(descriptor.taskId(), TaskStatus.FAILED);
        future.completeExceptionally(
            new TaskExecutionException("NoResult", "ExecuteTask stream ended without a result"));
      }
    } catch (Exception e) {
      try {
        store.updateStatus(descriptor.taskId(), TaskStatus.FAILED);
      } catch (RuntimeException ignored) {
        // store may already be inconsistent; still fail the future
      }
      future.completeExceptionally(e);
    }
  }

  private <T> T decodeResult(byte[] payload, Class<T> resultType)
      throws IOException, ClassNotFoundException {
    if (payload == null || payload.length == 0) {
      return null;
    }
    return codec.deserialize(payload, resultType);
  }

  public Optional<StoredTask> get(String taskId) {
    return store.get(taskId);
  }

  @Override
  public void close() {
    if (ownsExecutor && dispatchExecutor != null) {
      dispatchExecutor.shutdownNow();
    }
    if (dispatchClient != null) {
      dispatchClient.close();
    }
  }
}
