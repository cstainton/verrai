package dev.verrai.integration.service;

import dev.verrai.client.service.AsyncResult;
import dev.verrai.rpc.common.annotation.Service;
import dev.verrai.integration.service.dto.LoginRequest;

@Service
public interface LoginService {
    AsyncResult<String> login(LoginRequest request);
}
