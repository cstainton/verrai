package dev.verrai.demo.service;

import dev.verrai.demo.service.dto.*;
import dev.verrai.rpc.common.annotation.Service;

import dev.verrai.client.service.AsyncResult;
import dev.verrai.client.service.AsyncStreamResult;

public interface EchoService {
    AsyncResult<Greeting> echo(Greeting greeting);

    AsyncStreamResult<Greeting> streamGreetings(Greeting request);
}
