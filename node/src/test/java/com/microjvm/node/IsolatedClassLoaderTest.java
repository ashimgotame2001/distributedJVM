package com.microjvm.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.microjvm.node.execute.IsolatedTaskClassLoader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IsolatedClassLoaderTest {
  @TempDir Path temp;

  @Test
  void twoArtifactVersionsCoexistWithoutClash() throws Exception {
    Path v1Jar = temp.resolve("v1/label.jar");
    Path v2Jar = temp.resolve("v2/label.jar");
    TestArtifacts.writeVersionedJar(v1Jar, "v1");
    TestArtifacts.writeVersionedJar(v2Jar, "v2");

    try (IsolatedTaskClassLoader cl1 =
            new IsolatedTaskClassLoader(List.of(v1Jar), ClassLoader.getPlatformClassLoader());
        IsolatedTaskClassLoader cl2 =
            new IsolatedTaskClassLoader(List.of(v2Jar), ClassLoader.getPlatformClassLoader())) {
      Class<?> c1 = Class.forName("com.microjvm.tasks.versioned.Label", true, cl1);
      Class<?> c2 = Class.forName("com.microjvm.tasks.versioned.Label", true, cl2);
      assertNotSame(c1, c2);

      Method m1 = c1.getMethod("value");
      Method m2 = c2.getMethod("value");
      assertEquals("v1", m1.invoke(null));
      assertEquals("v2", m2.invoke(null));
    }
  }
}
