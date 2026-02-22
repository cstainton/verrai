package dev.verrai.client.service.dummy;

import dev.verrai.client.service.transport.ServiceDispatcher;
import dev.verrai.client.service.proto.RpcPacket;
import dev.verrai.rpc.common.transport.Transport;

public class TestService_Dispatcher implements ServiceDispatcher {
    @Override
    public void dispatch(RpcPacket packet, Object implementation, Transport transport) {
        // No-op for test
    }
}
