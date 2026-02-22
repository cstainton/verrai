package dev.verrai.integration.service;

import dev.verrai.client.service.AsyncResult;
import dev.verrai.client.service.dto.LogonResponse;
import dev.verrai.client.service.dto.LogonRequest;
import dev.verrai.rpc.common.annotation.Service;

@Service
public interface AuthenticationService {
    AsyncResult<LogonResponse> login(LogonRequest request);
}
