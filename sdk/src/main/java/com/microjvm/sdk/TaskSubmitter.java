package com.microjvm.sdk;

import com.microjvm.common.TaskDescriptor;
import java.io.IOException;

public interface TaskSubmitter {
  <T> MicroFuture<T> submit(MicroTask<T> task) throws IOException;

  <T> MicroFuture<T> submit(TaskDescriptor descriptor, Class<T> resultType);
}
