package com.microjvm.runtime;

import com.microjvm.common.TaskStatus;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryTaskStore implements TaskStore {
  private final ConcurrentMap<String, StoredTask> tasks = new ConcurrentHashMap<>();

  @Override
  public void put(StoredTask task) {
    tasks.put(task.taskId(), task);
  }

  @Override
  public Optional<StoredTask> get(String taskId) {
    return Optional.ofNullable(tasks.get(taskId));
  }

  @Override
  public StoredTask updateStatus(String taskId, TaskStatus status) {
    return tasks.compute(
        taskId,
        (id, existing) -> {
          if (existing == null) {
            throw new IllegalArgumentException("Unknown taskId: " + id);
          }
          return existing.withStatus(status);
        });
  }

  @Override
  public StoredTask update(String taskId, StoredTask task) {
    tasks.put(taskId, task);
    return task;
  }
}
