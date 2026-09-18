package com.microjvm.sdk;

import com.microjvm.common.TaskDescriptor;
import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.common.codec.TaskCodec;
import java.io.IOException;
import java.util.Objects;

public final class MicroJvm {
  private final TaskSubmitter submitter;
  private final TaskCodec codec;

  public MicroJvm(TaskSubmitter submitter) {
    this(submitter, new JavaSerializationCodec());
  }

  public MicroJvm(TaskSubmitter submitter, TaskCodec codec) {
    this.submitter = Objects.requireNonNull(submitter, "submitter");
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  public TaskCodec codec() {
    return codec;
  }

  public <T> MicroFuture<T> submit(MicroTask<T> task) throws IOException {
    return submitter.submit(task);
  }

  public <T> MicroFuture<T> submit(TaskDescriptor descriptor, Class<T> resultType) {
    return submitter.submit(descriptor, resultType);
  }
}
