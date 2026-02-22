package dev.verrai.integration.service;

import dev.verrai.integration.service.dto.*;

import dev.verrai.rpc.common.annotation.Service;

import dev.verrai.client.service.AsyncResult;

@Service
public interface MyDataService {
    AsyncResult<Void> processData(MyData data);

    AsyncResult<Employee> getBoss(Organization org);
}
