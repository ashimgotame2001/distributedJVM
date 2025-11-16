package com.distributed.jvm.shared.tasks;

import com.distributed.jvm.shared.DistributedTask;

import java.io.Serializable;

public class TongTask implements DistributedTask<String>, Serializable {
    @Override
    public String run() {
        return "Tong";
    }

    @Override
    public int getPriority() {
        return 4;
    }
}

