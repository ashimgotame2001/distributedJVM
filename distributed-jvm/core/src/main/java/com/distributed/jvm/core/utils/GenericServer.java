package com.distributed.jvm.core.utils;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GenericServer {

    private final int port;
    private final Server server;
    private final ExecutorService taskPool;

    public GenericServer(int port, GenericTaskService taskService) {
        this.port = port;
        this.taskPool = Executors.newFixedThreadPool(4);
        this.server = ServerBuilder.forPort(port)
                .addService(taskService)
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {
        if (server != null) server.shutdown();
        taskPool.shutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) server.awaitTermination();
    }

    public ExecutorService getTaskPool() {
        return taskPool;
    }
}
