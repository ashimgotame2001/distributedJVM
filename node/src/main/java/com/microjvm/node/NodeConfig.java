package com.microjvm.node;

import com.microjvm.common.codec.JavaSerializationCodec;
import com.microjvm.common.codec.TaskCodec;
import com.microjvm.node.artifact.LocalArtifactResolver;
import com.microjvm.node.execute.ReflectiveTaskInvoker;
import com.microjvm.node.execute.TaskWorkerPool;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NodeProperties.class)
public class NodeConfig {

  @Bean(destroyMethod = "close")
  TaskWorkerPool taskWorkerPool(NodeProperties properties) {
    return new TaskWorkerPool(properties.getWorkerThreads());
  }

  @Bean
  LocalArtifactResolver localArtifactResolver(NodeProperties properties) {
    return new LocalArtifactResolver(Path.of(properties.getArtifactRoot()));
  }

  @Bean
  TaskCodec taskCodec() {
    return new JavaSerializationCodec();
  }

  @Bean
  ReflectiveTaskInvoker reflectiveTaskInvoker(TaskCodec taskCodec) {
    return new ReflectiveTaskInvoker(taskCodec);
  }
}
