package uk.co.instanto.client.service;

import dev.verrai.rpc.common.serialization.Serializer;
import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.Transport;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.co.instanto.client.service.proto.RpcPacket;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RpcClientTest {

    @Mock
    private Transport transport;

    @Mock
    private Serializer serializer;

    private RpcClient rpcClient;
    private ArgumentCaptor<MessageHandler> messageHandlerCaptor;

    @Before
    public void setup() {
        messageHandlerCaptor = ArgumentCaptor.forClass(MessageHandler.class);
        rpcClient = new RpcClient(transport);
        rpcClient.setSerializer(serializer);

        verify(transport).addMessageHandler(messageHandlerCaptor.capture());
    }

    @Test
    public void testInvokeStub() throws Exception {
        String serviceId = "myService";
        String methodId = "myMethod";
        String requestPayload = "request";
        byte[] serializedRequest = "serializedRequest".getBytes();

        when(serializer.encode(requestPayload)).thenReturn(serializedRequest);

        AsyncResult<String> result = rpcClient.invokeStub(serviceId, methodId, new Object[]{requestPayload});

        ArgumentCaptor<byte[]> packetCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(packetCaptor.capture());

        RpcPacket packet = RpcPacket.ADAPTER.decode(packetCaptor.getValue());
        assertEquals(serviceId, packet.serviceId);
        assertEquals(methodId, packet.methodName);
        assertArrayEquals(serializedRequest, packet.payload.toByteArray());
        assertEquals(RpcPacket.Type.REQUEST, packet.type);
        assertNotNull(packet.requestId);

        assertNotNull(result);
    }

    @Test
    public void testHandleIncomingResponse() throws Exception {
        String serviceId = "myService";
        String methodId = "myMethod";
        String requestPayload = "request";
        byte[] serializedRequest = "serializedRequest".getBytes();
        when(serializer.encode(requestPayload)).thenReturn(serializedRequest);

        // 1. Invoke stub to register pending request
        AsyncResult<byte[]> result = rpcClient.invokeStub(serviceId, methodId, new Object[]{requestPayload});

        ArgumentCaptor<byte[]> packetCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(packetCaptor.capture());
        RpcPacket sentPacket = RpcPacket.ADAPTER.decode(packetCaptor.getValue());
        String requestId = sentPacket.requestId;

        // 2. Prepare result holder
        AtomicReference<byte[]> receivedValue = new AtomicReference<>();
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        result.thenAccept(receivedValue::set).exceptionally(err -> {
            receivedError.set(err);
            return null;
        });

        // 3. Simulate incoming response
        byte[] responsePayload = "response".getBytes();
        RpcPacket responsePacket = new RpcPacket.Builder()
                .requestId(requestId)
                .type(RpcPacket.Type.RESPONSE)
                .payload(ByteString.of(responsePayload))
                .build();

        messageHandlerCaptor.getValue().onMessage(RpcPacket.ADAPTER.encode(responsePacket));

        // 4. Verify
        assertArrayEquals(responsePayload, receivedValue.get());
        assertTrue(receivedError.get() == null);
    }

    @Test
    public void testHandleIncomingError() throws Exception {
        String serviceId = "myService";
        String methodId = "myMethod";
        String requestPayload = "request";
        byte[] serializedRequest = "serializedRequest".getBytes();
        when(serializer.encode(requestPayload)).thenReturn(serializedRequest);

        // 1. Invoke stub
        AsyncResult<byte[]> result = rpcClient.invokeStub(serviceId, methodId, new Object[]{requestPayload});

        ArgumentCaptor<byte[]> packetCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(packetCaptor.capture());
        RpcPacket sentPacket = RpcPacket.ADAPTER.decode(packetCaptor.getValue());
        String requestId = sentPacket.requestId;

        // 2. Prepare result holder
        AtomicReference<byte[]> receivedValue = new AtomicReference<>();
        AtomicReference<Throwable> receivedError = new AtomicReference<>();
        result.thenAccept(receivedValue::set).exceptionally(err -> {
            receivedError.set(err);
            return null;
        });

        // 3. Simulate incoming error
        String errorMessage = "Something went wrong";
        RpcPacket errorPacket = new RpcPacket.Builder()
                .requestId(requestId)
                .type(RpcPacket.Type.ERROR)
                .payload(ByteString.encodeUtf8(errorMessage))
                .build();

        messageHandlerCaptor.getValue().onMessage(RpcPacket.ADAPTER.encode(errorPacket));

        // 4. Verify
        assertTrue(receivedValue.get() == null);
        assertNotNull(receivedError.get());
        assertTrue(receivedError.get() instanceof RuntimeException);
        assertEquals(errorMessage, receivedError.get().getMessage());
    }
}
