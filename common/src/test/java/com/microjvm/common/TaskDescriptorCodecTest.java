package com.microjvm.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.common.codec.TaskCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskDescriptorCodecTest {
  @Test
  void descriptorRoundTripsThroughJavaSerialization() throws Exception {
    TaskDescriptor original =
        TaskDescriptor.builder()
            .taskId("task-42")
            .codeCoordinates(new ArtifactCoordinates("com.example", "work", "1.0.0"))
            .entry(new EntryPoint("com.example.Work", "run"))
            .args(new byte[] {1, 2, 3})
            .constraints(new TaskConstraints(512, false, 1))
            .dependencies(List.of("upstream-1"))
            .resultSpec(ResultSpec.BY_VALUE)
            .build();

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
      out.writeObject(original);
    }

    TaskDescriptor restored;
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
      restored = (TaskDescriptor) in.readObject();
    }

    assertEquals(original.taskId(), restored.taskId());
    assertEquals(original.codeCoordinates(), restored.codeCoordinates());
    assertEquals(original.entry(), restored.entry());
    assertArrayEquals(original.args(), restored.args());
    assertEquals(original.constraints().minHeapMb(), restored.constraints().minHeapMb());
    assertEquals(original.dependencies(), restored.dependencies());
    assertEquals(original.resultSpec(), restored.resultSpec());
  }

  @Test
  void argsCodecRoundTrips() throws Exception {
    TaskCodec codec = new JavaSerializationCodec();
    Object[] args = new Object[] {"hello", 7};
    byte[] bytes = codec.serialize(args);
    Object[] restored = codec.deserialize(bytes, Object[].class);
    assertEquals("hello", restored[0]);
    assertEquals(7, restored[1]);
  }
}
