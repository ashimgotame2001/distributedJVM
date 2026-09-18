package com.microjvm.sdk;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MicroFuture<T> {
  private final String taskId;
  private final CompletableFuture<T> delegate;

  public MicroFuture(String taskId) {
    this(taskId, new CompletableFuture<>());
  }

  public MicroFuture(String taskId, CompletableFuture<T> delegate) {
    this.taskId = Objects.requireNonNull(taskId, "taskId");
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  public String getTaskId() {
    return taskId;
  }

  public boolean isDone() {
    return delegate.isDone();
  }

  public T get() throws InterruptedException, ExecutionException {
    return delegate.get();
  }

  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.get(timeout, unit);
  }

  public CompletableFuture<T> toCompletableFuture() {
    return delegate;
  }

  public void complete(T value) {
    delegate.complete(value);
  }

  public void completeExceptionally(Throwable error) {
    delegate.completeExceptionally(error);
  }
}
