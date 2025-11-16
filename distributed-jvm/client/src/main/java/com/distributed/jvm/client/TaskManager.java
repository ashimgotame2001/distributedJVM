package com.distributed.jvm.client;

import com.distributed.jvm.core.utils.ServerConfigUtil;
import com.distributed.jvm.grpc.CoordinateRequest;
import com.distributed.jvm.grpc.CoordinatorResponse;
import com.distributed.jvm.grpc.CoordinatorServiceGrpc;
import com.distributed.jvm.shared.TaskSerializationUtils;
import com.distributed.jvm.shared.tasks.PingTask;
import com.distributed.jvm.shared.tasks.PongTask;
import com.distributed.jvm.shared.tasks.TingTask;
import com.distributed.jvm.shared.tasks.TongTask;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;

public class TaskManager {


    private final CoordinatorServiceGrpc.CoordinatorServiceBlockingStub stub;

    public TaskManager(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        stub = CoordinatorServiceGrpc.newBlockingStub(channel);
    }

    public Object submitTask(byte[] taskBytes) throws Exception {
        CoordinateRequest request = CoordinateRequest.newBuilder()
                .setTaskBytes(ByteString.copyFrom(taskBytes))
                .build();
        CoordinatorResponse response = stub.coordinateTask(request);
        return TaskSerializationUtils.deserialize(response.getResult().toByteArray());
    }

    public static void main(String[] args) throws Exception {
        ServerConfigUtil.loadPropertiesFromClasspath("application.properties");
        String masterNodeHost = ServerConfigUtil.getFallbackHost();
        String masterNodePort = ServerConfigUtil.getFallbackPort();
        TaskManager client = new TaskManager(masterNodeHost, Integer.parseInt(masterNodePort));
        List<byte[]> tasks = List.of(
                TaskSerializationUtils.serialize(new PingTask()),
                TaskSerializationUtils.serialize(new PongTask()),
                TaskSerializationUtils.serialize(new TingTask()),
                TaskSerializationUtils.serialize(new TongTask())
        );

        tasks.parallelStream().forEach(taskBytes -> {
            try {
                Object result = client.submitTask(taskBytes);
                System.out.println("Task result: " + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
