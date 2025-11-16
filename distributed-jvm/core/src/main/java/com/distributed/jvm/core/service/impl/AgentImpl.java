package com.distributed.jvm.core.service.impl;

import com.distributed.jvm.core.service.GeneralAgent;
import com.distributed.jvm.core.utils.GenericServer;
import com.distributed.jvm.core.utils.GenericTaskService;

public class AgentImpl implements GeneralAgent {
    @Override
    public GenericServer manageAgent(int port, int poolSize) {
        GenericTaskService taskService = new GenericTaskService(poolSize);
        return new GenericServer(port, taskService);
    }
}
