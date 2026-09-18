package com.microjvm.runtime;

import com.microjvm.common.TaskStatus;
import java.util.Optional;

public interface TaskStore {
  void put(StoredTask task);

  Optional<StoredTask> get(String taskId);

  StoredTask updateStatus(String taskId, TaskStatus status);

  StoredTask update(String taskId, StoredTask task);
}
