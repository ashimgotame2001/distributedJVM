package com.distributed.jvm.shared;

import java.io.Serializable;

public interface DistributedTask<T> extends Serializable,Comparable<DistributedTask<T>> {
    T run() throws Exception;
    int getPriority();

    @Override
    default int compareTo(DistributedTask<T> other) {
        return Integer.compare(other.getPriority(), this.getPriority());
    }
}
