package com.microjvm.sdk;

public final class TaskExecutionException extends RuntimeException {
  private final String errorType;

  public TaskExecutionException(String errorType, String message) {
    super(message);
    this.errorType = errorType != null ? errorType : "Error";
  }

  public TaskExecutionException(String errorType, String message, Throwable cause) {
    super(message, cause);
    this.errorType = errorType != null ? errorType : "Error";
  }

  public String errorType() {
    return errorType;
  }
}
