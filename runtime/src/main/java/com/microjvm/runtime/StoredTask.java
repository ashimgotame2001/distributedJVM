package com.microjvm.runtime;

import com.microjvm.common.TaskDescriptor;
import com.microjvm.common.TaskStatus;
import java.util.Objects;

public final class StoredTask {
  private final TaskDescriptor descriptor;
  private final TaskStatus status;

  public StoredTask(TaskDescriptor descriptor, TaskStatus status) {
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.status = Objects.requireNonNull(status, "status");
  }

  public TaskDescriptor descriptor() {
    return descriptor;
  }

  public TaskStatus status() {
    return status;
  }

  public String taskId() {
    return descriptor.taskId();
  }

  public StoredTask withStatus(TaskStatus next) {
    return new StoredTask(descriptor, next);
  }
}
