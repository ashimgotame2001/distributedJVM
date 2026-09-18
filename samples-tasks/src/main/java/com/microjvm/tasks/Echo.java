package com.microjvm.tasks;

public final class Echo {
  private Echo() {}

  public static String echo(String message) {
    return message;
  }

  public static String fail(String message) {
    throw new IllegalStateException(message);
  }

  public static int add(int a, int b) {
    return a + b;
  }
}
