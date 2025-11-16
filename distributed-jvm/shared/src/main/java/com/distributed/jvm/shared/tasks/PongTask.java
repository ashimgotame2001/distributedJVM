package com.distributed.jvm.shared.tasks;

import com.distributed.jvm.shared.DistributedTask;

import java.io.Serializable;

public class PongTask implements DistributedTask<String>, Serializable {

    @Override
    public String run() {
        return "pong";
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
