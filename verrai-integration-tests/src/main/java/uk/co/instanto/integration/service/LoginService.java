package uk.co.instanto.integration.service;

import uk.co.instanto.client.service.AsyncResult;
import dev.verrai.rpc.common.annotation.Service;
import uk.co.instanto.integration.service.dto.LoginRequest;

@Service
public interface LoginService {
    AsyncResult<String> login(LoginRequest request);
}
