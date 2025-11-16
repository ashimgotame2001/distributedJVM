package com.distributed.jvm.shared.tasks;

import com.distributed.jvm.shared.DistributedTask;

import java.io.Serializable;

public class PingTask implements DistributedTask<String>, Serializable {
    @Override
    public String run() {
        return "ping";
    }

    @Override
    public int getPriority() {
        return 1;
    }
}

