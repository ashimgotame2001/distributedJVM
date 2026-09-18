package com.microjvm.node.execute;

import com.microjvm.common.codec.TaskCodec;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

public final class ReflectiveTaskInvoker {
  private final TaskCodec codec;

  public ReflectiveTaskInvoker(TaskCodec codec) {
    this.codec = Objects.requireNonNull(codec, "codec");
  }

  public Object invoke(ClassLoader classLoader, String className, String methodName, byte[] argsBytes)
      throws Exception {
    Class<?> type = Class.forName(className, true, classLoader);
    Object[] args =
        argsBytes == null || argsBytes.length == 0
            ? new Object[0]
            : codec.deserialize(argsBytes, Object[].class);
    Method method = resolveMethod(type, methodName, args);
    Object target = Modifier.isStatic(method.getModifiers()) ? null : type.getDeclaredConstructor().newInstance();
    method.setAccessible(true);
    return method.invoke(target, args);
  }

  private static Method resolveMethod(Class<?> type, String methodName, Object[] args)
      throws NoSuchMethodException {
    Method match = null;
    for (Method method : type.getDeclaredMethods()) {
      if (!method.getName().equals(methodName)) {
        continue;
      }
      Class<?>[] params = method.getParameterTypes();
      if (params.length != args.length) {
        continue;
      }
      if (compatible(params, args)) {
        if (match != null) {
          throw new NoSuchMethodException("Ambiguous method " + methodName + " on " + type.getName());
        }
        match = method;
      }
    }
    if (match == null) {
      throw new NoSuchMethodException(
          type.getName()
              + "#"
              + methodName
              + Arrays.toString(Arrays.stream(args).map(a -> a == null ? "null" : a.getClass().getName()).toArray()));
    }
    return match;
  }

  private static boolean compatible(Class<?>[] params, Object[] args) {
    for (int i = 0; i < params.length; i++) {
      Object arg = args[i];
      if (arg == null) {
        if (params[i].isPrimitive()) {
          return false;
        }
        continue;
      }
      if (!wrap(params[i]).isAssignableFrom(arg.getClass())) {
        return false;
      }
    }
    return true;
  }

  private static Class<?> wrap(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == char.class) {
      return Character.class;
    }
    return type;
  }
}
