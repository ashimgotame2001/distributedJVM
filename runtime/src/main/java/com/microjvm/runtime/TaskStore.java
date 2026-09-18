package com.microjvm.runtime;

import java.util.Optional;

public interface TaskStore {
  void put(StoredTask task);

  Optional<StoredTask> get(String taskId);

  StoredTask updateStatus(String taskId, com.microjvm.common.TaskStatus status);
}
