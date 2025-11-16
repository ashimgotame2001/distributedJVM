package com.distributed.jvm.agent.service;

import com.distributed.jvm.grpc.*;
import io.grpc.stub.StreamObserver;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

public class NodeHealthService extends NodeAgentServiceGrpc.NodeAgentServiceImplBase {

    private final String nodeId;
    private final String host;
    private final int port;
    private final OperatingSystemMXBean osBean;

    public NodeHealthService(String nodeId, String host, int port) {
        this.nodeId = nodeId;
        this.host = host;
        this.port = port;
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public void getNodeHealth(EmptyRequest request, StreamObserver<NodeHealthResponse> responseObserver) {
        try {
            long heartbeat = Instant.now().toEpochMilli();

            double systemCpuLoad = safeCpuLoad(osBean.getSystemCpuLoad());
            double processCpuLoad = safeCpuLoad(osBean.getProcessCpuLoad());
            int processors = osBean.getAvailableProcessors();

            CpuMetrics cpuMetrics = CpuMetrics.newBuilder()
                    .setSystemCpuLoad(systemCpuLoad)
                    .setProcessCpuLoad(processCpuLoad)
                    .setAvailableProcessors(processors)
                    .build();

            NodeHealthResponse response = NodeHealthResponse.newBuilder()
                    .setNodeId(nodeId)
                    .setHost(host)
                    .setPort(port)
                    .setStatus(NodeStatus.READY)
                    .setLastHeartbeat(heartbeat)
                    .setCpu(cpuMetrics)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            System.out.printf("✅ Health Report from %s:%d | CPU: %.2f%% sys, %.2f%% proc%n",
                    host, port, systemCpuLoad * 100, processCpuLoad * 100);

        } catch (Exception e) {
            e.printStackTrace();
            responseObserver.onError(e);
        }
    }

    private double safeCpuLoad(double value) {
        if (Double.isNaN(value) || value < 0) return 0.0;
        return value;
    }

    private String getHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
