package com.microjvm.common.codec;

import java.io.IOException;

public interface TaskCodec {
  byte[] serialize(Object value) throws IOException;

  <T> T deserialize(byte[] bytes, Class<T> type) throws IOException, ClassNotFoundException;
}
