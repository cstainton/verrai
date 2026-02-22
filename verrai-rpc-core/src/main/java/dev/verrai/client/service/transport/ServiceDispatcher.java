package dev.verrai.client.service.transport;

import dev.verrai.client.service.proto.RpcPacket;
import dev.verrai.rpc.common.transport.Transport;

public interface ServiceDispatcher {
    void dispatch(RpcPacket packet, Object implementation, Transport transport);
}
