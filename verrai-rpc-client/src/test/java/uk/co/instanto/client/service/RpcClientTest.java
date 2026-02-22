package uk.co.instanto.client.service;

import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.Transport;
import dev.verrai.rpc.common.serialization.Serializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.co.instanto.client.service.proto.RpcPacket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        // Capture the message handler registered by RpcClient
        ArgumentCaptor<MessageHandler> handlerCaptor = ArgumentCaptor.forClass(MessageHandler.class);
        verify(transport).addMessageHandler(handlerCaptor.capture());
        messageHandler = handlerCaptor.getValue();

        // Default serializer behavior
        when(serializer.encode(any())).thenReturn(new byte[]{1, 2, 3});
    }

    @Test
    public void testInvokeStreamStubSendsRequest() {
        String serviceId = "TestService";
        String methodId = "streamData";
        Object[] args = new Object[]{"request"};

        AsyncStreamResult<Object> result = rpcClient.invokeStreamStub(serviceId, methodId, args);

        assertNotNull(result);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(captor.capture());

        byte[] sentData = captor.getValue();
        // Decode to verify content
        try {
            RpcPacket packet = RpcPacket.ADAPTER.decode(sentData);
            assertEquals(serviceId, packet.serviceId);
            assertEquals(methodId, packet.methodName);
            assertEquals(RpcPacket.Type.REQUEST, packet.type);
            assertNotNull(packet.requestId);
        } catch (Exception e) {
            fail("Failed to decode sent packet: " + e.getMessage());
        }
    }

    @Test
    public void testStreamDataReception() throws Exception {
        String serviceId = "TestService";
        String methodId = "streamData";
        Object[] args = new Object[]{"request"};

        AsyncStreamResult<byte[]> result = rpcClient.invokeStreamStub(serviceId, methodId, args);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(captor.capture());
        RpcPacket requestPacket = RpcPacket.ADAPTER.decode(captor.getValue());
        String requestId = requestPacket.requestId;

        AtomicReference<byte[]> receivedData = new AtomicReference<>();
        result.subscribe(receivedData::set, e -> {}, () -> {});

        // Simulate incoming STREAM_DATA
        byte[] payload = new byte[]{10, 20, 30};
        RpcPacket dataPacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.STREAM_DATA)
                .requestId(requestId)
                .payload(okio.ByteString.of(payload))
                .build();

        messageHandler.onMessage(RpcPacket.ADAPTER.encode(dataPacket));

        assertArrayEquals(payload, receivedData.get());
    }

    @Test
    public void testStreamError() throws Exception {
        String serviceId = "TestService";
        String methodId = "streamData";
        Object[] args = new Object[]{"request"};

        AsyncStreamResult<byte[]> result = rpcClient.invokeStreamStub(serviceId, methodId, args);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(captor.capture());
        String requestId = RpcPacket.ADAPTER.decode(captor.getValue()).requestId;

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        result.subscribe(d -> {}, errorRef::set, () -> {});

        // Simulate STREAM_ERROR
        String errorMessage = "Something went wrong";
        RpcPacket errorPacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.STREAM_ERROR)
                .requestId(requestId)
                .payload(okio.ByteString.encodeUtf8(errorMessage))
                .build();

        messageHandler.onMessage(RpcPacket.ADAPTER.encode(errorPacket));

        assertNotNull(errorRef.get());
        assertEquals(errorMessage, errorRef.get().getMessage());
    }

    @Test
    public void testStreamComplete() throws Exception {
        String serviceId = "TestService";
        String methodId = "streamData";
        Object[] args = new Object[]{"request"};

        AsyncStreamResult<byte[]> result = rpcClient.invokeStreamStub(serviceId, methodId, args);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(captor.capture());
        String requestId = RpcPacket.ADAPTER.decode(captor.getValue()).requestId;

        AtomicBoolean completed = new AtomicBoolean(false);
        result.subscribe(d -> {}, e -> {}, () -> completed.set(true));

        // Simulate STREAM_COMPLETE
        RpcPacket completePacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.STREAM_COMPLETE)
                .requestId(requestId)
                .build();

        messageHandler.onMessage(RpcPacket.ADAPTER.encode(completePacket));

        assertTrue(completed.get());
    }

    @Test
    public void testStreamRequestN() throws Exception {
        String serviceId = "TestService";
        String methodId = "streamData";
        Object[] args = new Object[]{"request"};

        AsyncStreamResult<byte[]> result = rpcClient.invokeStreamStub(serviceId, methodId, args);

        // Capture initial request to get ID
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(transport).send(captor.capture());
        String requestId = RpcPacket.ADAPTER.decode(captor.getValue()).requestId;

        // Reset transport to clear previous send
        reset(transport);

        // Subscribe with Flow.Subscriber to trigger request(n)
        java.util.concurrent.Flow.Subscriber<byte[]> subscriber = new java.util.concurrent.Flow.Subscriber<>() {
            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                subscription.request(5);
            }
            @Override public void onNext(byte[] item) {}
            @Override public void onError(Throwable throwable) {}
            @Override public void onComplete() {}
        };

        result.subscribe(subscriber);

        // Verify STREAM_REQUEST_N packet sent
        verify(transport).send(captor.capture());
        RpcPacket requestNPacket = RpcPacket.ADAPTER.decode(captor.getValue());

        assertEquals(RpcPacket.Type.STREAM_REQUEST_N, requestNPacket.type);
        assertEquals(requestId, requestNPacket.requestId);
        assertEquals("5", requestNPacket.payload.utf8());
    }
}
