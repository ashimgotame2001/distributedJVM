package com.microjvm.runtime;

import com.microjvm.common.TaskDescriptor;
import com.microjvm.common.TaskStatus;
import java.util.Objects;

public final class StoredTask {
  private final TaskDescriptor descriptor;
  private final TaskStatus status;
  private final NodeTarget placedOn;

  public StoredTask(TaskDescriptor descriptor, TaskStatus status) {
    this(descriptor, status, null);
  }

  public StoredTask(TaskDescriptor descriptor, TaskStatus status, NodeTarget placedOn) {
    this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    this.status = Objects.requireNonNull(status, "status");
    this.placedOn = placedOn;
  }

  public TaskDescriptor descriptor() {
    return descriptor;
  }

  public TaskStatus status() {
    return status;
  }

  public NodeTarget placedOn() {
    return placedOn;
  }

  public String taskId() {
    return descriptor.taskId();
  }

  public StoredTask withStatus(TaskStatus next) {
    return new StoredTask(descriptor, next, placedOn);
  }

  public StoredTask withPlacement(NodeTarget target, TaskStatus status) {
    return new StoredTask(descriptor, status, target);
  }
}
