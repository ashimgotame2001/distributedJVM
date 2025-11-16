package com.distributed.jvm.shared.tasks;

import com.distributed.jvm.shared.DistributedTask;

import java.io.Serializable;

public class TingTask implements DistributedTask<String>, Serializable {
    @Override
    public String run() {
        return "ting";
    }

    @Override
    public int getPriority() {
        return 2;
    }
}

