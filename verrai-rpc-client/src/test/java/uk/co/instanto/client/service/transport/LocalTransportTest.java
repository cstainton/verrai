package uk.co.instanto.client.service.transport;

import dev.verrai.rpc.common.transport.MessageHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class LocalTransportTest {

    private LocalTransport transport1;
    private LocalTransport transport2;

    @Mock
    private MessageHandler handler1;

    @Mock
    private MessageHandler handler2;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        transport1 = new LocalTransport();
        transport2 = new LocalTransport();
    }

    @Test
    public void testConnectAndSend() {
        transport1.connect(transport2);
        transport2.addMessageHandler(handler2);

        byte[] data = new byte[]{1, 2, 3};
        transport1.send(data);

        verify(handler2).onMessage(data);
        verifyNoInteractions(handler1);
    }

    @Test
    public void testBidirectionalCommunication() {
        transport1.connect(transport2);
        transport2.connect(transport1);

        transport1.addMessageHandler(handler1);
        transport2.addMessageHandler(handler2);

        byte[] data1 = new byte[]{1};
        byte[] data2 = new byte[]{2};

        transport1.send(data1);
        transport2.send(data2);

        verify(handler2).onMessage(data1);
        verify(handler1).onMessage(data2);
    }

    @Test
    public void testMultipleHandlers() {
        transport1.connect(transport2);

        MessageHandler handler3 = mock(MessageHandler.class);
        transport2.addMessageHandler(handler2);
        transport2.addMessageHandler(handler3);

        byte[] data = new byte[]{10};
        transport1.send(data);

        verify(handler2).onMessage(data);
        verify(handler3).onMessage(data);
    }

    @Test
    public void testSendWithoutPeer() {
        // Should not throw exception
        try {
            transport1.send(new byte[]{1});
        } catch (Exception e) {
            fail("Should not throw exception when sending without peer: " + e.getMessage());
        }
    }

    @Test
    public void testHandlerModificationSafety() {
        transport1.connect(transport2);

        AtomicBoolean nestedHandlerCalled = new AtomicBoolean(false);

        // A handler that adds another handler when invoked
        MessageHandler modifyingHandler = new MessageHandler() {
            @Override
            public void onMessage(byte[] data) {
                transport2.addMessageHandler(d -> nestedHandlerCalled.set(true));
            }
        };

        transport2.addMessageHandler(modifyingHandler);

        // First send: invoking modifyingHandler should add the new handler,
        // but the new handler should NOT be called in THIS delivery loop (snapshot iterator)
        // AND it should not throw ConcurrentModificationException
        try {
            transport1.send(new byte[]{1});
        } catch (Exception e) {
            fail("Should not throw exception during handler modification: " + e.getMessage());
        }

        // The nested handler should NOT have been called yet because it was added during iteration
        // over the snapshot
        if (nestedHandlerCalled.get()) {
             fail("Nested handler should NOT have been called during the same delivery cycle");
        }

        // Send again to verify the new handler is now active
        transport1.send(new byte[]{2});

        if (!nestedHandlerCalled.get()) {
            fail("Nested handler should have been called on second send");
        }
    }
}
