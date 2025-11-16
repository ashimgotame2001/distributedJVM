package com.distributed.jvm.core.service;

import com.distributed.jvm.core.utils.GenericServer;

public interface GeneralAgent {

    GenericServer manageAgent(int port, int poolSize) throws Exception;
}
