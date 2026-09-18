package com.microjvm.runtime;

import com.microjvm.common.TaskDescriptor;
import com.microjvm.common.TaskStatus;
import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.common.codec.TaskCodec;
import com.microjvm.sdk.MicroFuture;
import com.microjvm.sdk.MicroTask;
import com.microjvm.sdk.TaskSubmitter;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public final class LocalRuntime implements TaskSubmitter {
  private final TaskStore store;
  private final TaskCodec codec;

  public LocalRuntime() {
    this(new InMemoryTaskStore(), new JavaSerializationCodec());
  }

  public LocalRuntime(TaskStore store, TaskCodec codec) {
    this.store = Objects.requireNonNull(store, "store");
    this.codec = Objects.requireNonNull(codec, "codec");
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
    return new MicroFuture<>(descriptor.taskId());
  }

  public Optional<StoredTask> get(String taskId) {
    return store.get(taskId);
  }
}
