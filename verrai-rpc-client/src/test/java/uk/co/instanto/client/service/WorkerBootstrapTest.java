package uk.co.instanto.client.service;

import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.co.instanto.client.service.proto.RpcPacket;
import uk.co.instanto.client.service.transport.ServiceDispatcher;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class WorkerBootstrapTest {

    @Mock
    private Transport transport;

    @Mock
    private ServiceDispatcher dispatcher;

    private WorkerBootstrap workerBootstrap;
    private MessageHandler messageHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        workerBootstrap = new WorkerBootstrap(transport);

        ArgumentCaptor<MessageHandler> handlerCaptor = ArgumentCaptor.forClass(MessageHandler.class);
        verify(transport).addMessageHandler(handlerCaptor.capture());
        messageHandler = handlerCaptor.getValue();
    }

    @After
    public void tearDown() {
        // Clean up UnitRegistry singleton to avoid polluting other tests
        UnitRegistry.getInstance().reset();
    }

    @Test
    public void testHandleMessage_SuccessfulDispatch() {
        String serviceId = "TestService";
        String methodName = "testMethod";
        Object serviceInstance = new Object();

        // 1. Register service in UnitRegistry
        UnitRegistry.register(serviceId, serviceInstance);

        // 2. Register dispatcher in WorkerBootstrap
        workerBootstrap.registerDispatcher(serviceId, dispatcher);

        // 3. Create and encode RpcPacket
        RpcPacket packet = new RpcPacket.Builder()
                .serviceId(serviceId)
                .methodName(methodName)
                .build();
        byte[] bytes = RpcPacket.ADAPTER.encode(packet);

        // 4. Handle message
        messageHandler.onMessage(bytes);

        // 5. Verify dispatch
        verify(dispatcher).dispatch(any(RpcPacket.class), eq(serviceInstance), eq(transport));
    }

    @Test
    public void testHandleMessage_DispatcherNotFound() {
        String serviceId = "UnknownService";
        Object serviceInstance = new Object();

        // Register service but NO dispatcher
        UnitRegistry.register(serviceId, serviceInstance);

        RpcPacket packet = new RpcPacket.Builder()
                .serviceId(serviceId)
                .methodName("someMethod")
                .build();
        byte[] bytes = RpcPacket.ADAPTER.encode(packet);

        messageHandler.onMessage(bytes);

        verify(dispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    public void testHandleMessage_ServiceNotFound() {
        String serviceId = "MissingService";

        // Register dispatcher but NO service in UnitRegistry
        workerBootstrap.registerDispatcher(serviceId, dispatcher);

        RpcPacket packet = new RpcPacket.Builder()
                .serviceId(serviceId)
                .methodName("someMethod")
                .build();
        byte[] bytes = RpcPacket.ADAPTER.encode(packet);

        messageHandler.onMessage(bytes);

        verify(dispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    public void testHandleMessage_GarbageData() {
        byte[] garbage = new byte[]{1, 2, 3, 4, 5};

        // Should not throw exception (it catches Exception internally)
        messageHandler.onMessage(garbage);

        verify(dispatcher, never()).dispatch(any(), any(), any());
    }
}
