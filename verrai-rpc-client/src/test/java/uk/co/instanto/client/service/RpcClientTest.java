package uk.co.instanto.client.service;

import dev.verrai.rpc.common.serialization.Serializer;
import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.Transport;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.co.instanto.client.service.proto.RpcPacket;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RpcClientTest {

    @Mock
    private Transport transport;

    @Mock
    private Serializer serializer;

    private RpcClient rpcClient;
    private MessageHandler messageHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rpcClient = new RpcClient(transport);
        rpcClient.setSerializer(serializer);

        ArgumentCaptor<MessageHandler> handlerCaptor = ArgumentCaptor.forClass(MessageHandler.class);
        verify(transport).addMessageHandler(handlerCaptor.capture());
        messageHandler = handlerCaptor.getValue();

        // Default serializer behavior
        when(serializer.encode(any())).thenReturn(new byte[0]);
    }

    @Test
    public void testNetworkErrorHandling() throws Exception {
        String serviceId = "TestService";
        String methodId = "testMethod";
        Object[] args = new Object[]{"request"};
        String errorMessage = "Network Failure Simulated";

        // 1. Invoke stub to register pending request
        AsyncResult<Object> result = rpcClient.invokeStub(serviceId, methodId, args);

        // 2. Capture the sent packet to get the requestId
        ArgumentCaptor<byte[]> packetCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(packetCaptor.capture());
        RpcPacket sentPacket = RpcPacket.ADAPTER.decode(packetCaptor.getValue());
        String requestId = sentPacket.requestId;

        assertNotNull("RequestId should not be null", requestId);

        // 3. Simulate incoming ERROR packet
        RpcPacket errorPacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.ERROR)
                .requestId(requestId)
                .payload(ByteString.encodeUtf8(errorMessage))
                .serviceId(serviceId)
                .methodName(methodId)
                .build();

        byte[] errorBytes = RpcPacket.ADAPTER.encode(errorPacket);
        messageHandler.onMessage(errorBytes);

        // 4. Verify the result completed exceptionally
        final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
        result.exceptionally(ex -> {
            exceptionRef.set(ex);
            return null;
        });

        assertNotNull("Exception should have been received", exceptionRef.get());
        assertTrue("Exception message should contain the error message",
                exceptionRef.get().getMessage().contains(errorMessage));
    }

    @Test
    public void testStreamNetworkErrorHandling() throws Exception {
        String serviceId = "StreamService";
        String methodId = "streamMethod";
        Object[] args = new Object[]{"streamRequest"};
        String errorMessage = "Stream Failure Simulated";

        // 1. Invoke stream stub
        AsyncStreamResult<Object> result = rpcClient.invokeStreamStub(serviceId, methodId, args);

        // 2. Capture sent packet
        ArgumentCaptor<byte[]> packetCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(packetCaptor.capture());
        RpcPacket sentPacket = RpcPacket.ADAPTER.decode(packetCaptor.getValue());
        String requestId = sentPacket.requestId;

        // 3. Setup subscriber to catch error
        final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
        result.subscribe(
                val -> fail("Should not receive value"),
                exceptionRef::set,
                () -> fail("Should not complete")
        );

        // 4. Simulate incoming STREAM_ERROR packet
        RpcPacket errorPacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.STREAM_ERROR)
                .requestId(requestId)
                .payload(ByteString.encodeUtf8(errorMessage))
                .serviceId(serviceId)
                .methodName(methodId)
                .build();

        byte[] errorBytes = RpcPacket.ADAPTER.encode(errorPacket);
        messageHandler.onMessage(errorBytes);

        // 5. Verify error
        assertNotNull("Exception should have been received via stream callback", exceptionRef.get());
        assertTrue("Exception message should contain the error message",
                exceptionRef.get().getMessage().contains(errorMessage));
    }
}
