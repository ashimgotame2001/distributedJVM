package com.distributed.jvm.coordinator.models;

import com.distributed.jvm.grpc.NodeHealthResponse;
import com.distributed.jvm.grpc.NodeStatus;



public class RegisteredNodeInfo {

    private String nodeId;
    private NodeHealthResponse response;
    private NodeStatus status;

    public RegisteredNodeInfo(String nodeId, NodeHealthResponse response, NodeStatus status) {
        this.nodeId = nodeId;
        this.response = response;
        this.status = status;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public NodeHealthResponse getResponse() {
        return response;
    }

    public void setResponse(NodeHealthResponse response) {
        this.response = response;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }
}
