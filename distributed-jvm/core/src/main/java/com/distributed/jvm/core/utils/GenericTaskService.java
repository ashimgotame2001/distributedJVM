package com.distributed.jvm.core.utils;

import com.distributed.jvm.grpc.TaskRequest;
import com.distributed.jvm.grpc.TaskResponse;
import com.distributed.jvm.grpc.TaskServiceGrpc;
import com.distributed.jvm.shared.TaskSerializationUtils;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;

import java.io.Serializable;
import java.util.concurrent.*;

public class GenericTaskService extends TaskServiceGrpc.TaskServiceImplBase {
    private final ExecutorService executorService;

    public GenericTaskService(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void submitTask(TaskRequest request, StreamObserver<TaskResponse> responseObserver) {
        byte[] taskBytes = request.getTaskBytes().toByteArray();

        // Submit task to executor
        Future<Object> future = executorService.submit(() -> {
            try {
                // Deserialize the task
                Object task = TaskSerializationUtils.deserialize(taskBytes);

                // Execute the task based on its type
                return executeTask(task);

            } catch (Exception e) {
                throw new RuntimeException("Task execution failed", e);
            }
        });

        try {
            // Get the result with timeout
            Object result = future.get(30, TimeUnit.SECONDS);
            byte[] resultBytes = TaskSerializationUtils.serialize((Serializable) result);

            TaskResponse response = TaskResponse.newBuilder()
                    .setResult(ByteString.copyFrom(resultBytes))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (TimeoutException e) {
            future.cancel(true);
            responseObserver.onError(new RuntimeException("Task timeout"));
        } catch (Exception e) {
            responseObserver.onError(new RuntimeException("Task execution failed: " + e.getMessage()));
        }
    }

    private Object executeTask(Object task) {
        // Use reflection to call execute method
        try {
            if (task.getClass().getMethod("execute") != null) {
                return task.getClass().getMethod("execute").invoke(task);
            }
        } catch (Exception e) {
            // Fallback to toString if execute method doesn't exist
            return "Executed: " + task.toString();
        }
        return "No result from task: " + task.getClass().getSimpleName();
    }

    public void shutdown() {
        executorService.shutdown();
    }
}