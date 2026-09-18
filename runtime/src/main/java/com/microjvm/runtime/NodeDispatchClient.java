package com.microjvm.runtime;

import com.microjvm.common.TaskDescriptor;
import com.microjvm.proto.TaskEvent;
import java.util.Iterator;

public interface NodeDispatchClient extends AutoCloseable {
  Iterator<TaskEvent> execute(TaskDescriptor descriptor);

  @Override
  default void close() {}
}
