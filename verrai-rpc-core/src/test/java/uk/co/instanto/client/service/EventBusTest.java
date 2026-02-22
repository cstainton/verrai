package uk.co.instanto.client.service;

import dev.verrai.rpc.common.codec.Codec;
import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.Transport;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import uk.co.instanto.client.service.proto.EventPacket;
import uk.co.instanto.client.service.proto.RpcPacket;
import com.squareup.wire.ProtoAdapter;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class EventBusTest {

    private EventBus eventBus;
    private MockTransport transport;

    @Before
    public void setUp() {
        eventBus = new EventBus("test-publisher");
        transport = new MockTransport();
        eventBus.addTransport(transport);
    }

    @Test
    public void testLocalPublishAndSubscribe_ScopeLocal() {
        AtomicReference<TestEvent> received = new AtomicReference<>();
        eventBus.subscribe(TestEvent.class, received::set);

        TestEvent event = new TestEvent("hello");
        eventBus.publish(event, Scope.LOCAL);

        assertNotNull("Should receive event locally", received.get());
        assertEquals("hello", received.get().data);
        assertNull("Should not send to transport for local scope", transport.lastSentMessage);
    }

    @Test
    public void testRemotePublish() {
        eventBus.registerCodec(TestEvent.class, new TestEventCodec());

        TestEvent event = new TestEvent("remote-hello");
        eventBus.publish(event); // Default GLOBAL scope

        assertNotNull("Should send to transport", transport.lastSentMessage);

        // Verify sent message
        try {
            RpcPacket rpcPacket = RpcPacket.ADAPTER.decode(transport.lastSentMessage);
            assertEquals(RpcPacket.Type.EVENT, rpcPacket.type);

            EventPacket eventPacket = EventPacket.ADAPTER.decode(rpcPacket.payload);
            assertEquals("test-publisher", eventPacket.publisherId);
            assertEquals(TestEvent.class.getName(), eventPacket.eventType);

            // The payload of EventPacket is the encoded Wire message (RpcPacket in our mock codec)
            RpcPacket innerPacket = RpcPacket.ADAPTER.decode(eventPacket.payload);
            assertEquals("remote-hello", innerPacket.payload.utf8());

        } catch (Exception e) {
            fail("Failed to decode sent message: " + e.getMessage());
        }
    }

    @Test
    public void testRemoteReceive() throws Exception {
        eventBus.registerCodec(TestEvent.class, new TestEventCodec());

        AtomicReference<TestEvent> received = new AtomicReference<>();
        eventBus.subscribe(TestEvent.class, received::set);

        // Construct incoming message
        // 1. Wire message (RpcPacket acting as wire)
        RpcPacket wireMsg = new RpcPacket.Builder()
                .payload(ByteString.encodeUtf8("incoming-data"))
                .build();
        byte[] wireBytes = RpcPacket.ADAPTER.encode(wireMsg);

        // 2. EventPacket
        EventPacket eventPacket = new EventPacket.Builder()
                .eventType(TestEvent.class.getName())
                .payload(ByteString.of(wireBytes))
                .publisherId("remote-sender")
                .timestamp(System.currentTimeMillis())
                .build();
        byte[] eventPacketBytes = EventPacket.ADAPTER.encode(eventPacket);

        // 3. RpcPacket wrapper
        RpcPacket rpcPacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.EVENT)
                .requestId(UUID.randomUUID().toString())
                .payload(ByteString.of(eventPacketBytes))
                .build();
        byte[] finalBytes = RpcPacket.ADAPTER.encode(rpcPacket);

        // Simulate incoming
        transport.handler.onMessage(finalBytes);

        assertNotNull("Should receive remote event", received.get());
        assertEquals("incoming-data", received.get().data);
    }

    @Test
    public void testRemoteReceive_RejectLocalScope() throws Exception {
        eventBus.registerCodec(TestEvent.class, new TestEventCodec());
        eventBus.registerEventScope(TestEvent.class, Scope.LOCAL);

        AtomicReference<TestEvent> received = new AtomicReference<>();
        eventBus.subscribe(TestEvent.class, received::set);

        // Construct incoming message (same as above)
        RpcPacket wireMsg = new RpcPacket.Builder()
                .payload(ByteString.encodeUtf8("incoming-data"))
                .build();
        byte[] wireBytes = RpcPacket.ADAPTER.encode(wireMsg);

        EventPacket eventPacket = new EventPacket.Builder()
                .eventType(TestEvent.class.getName())
                .payload(ByteString.of(wireBytes))
                .publisherId("remote-sender")
                .timestamp(System.currentTimeMillis())
                .build();
        byte[] eventPacketBytes = EventPacket.ADAPTER.encode(eventPacket);

        RpcPacket rpcPacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.EVENT)
                .requestId(UUID.randomUUID().toString())
                .payload(ByteString.of(eventPacketBytes))
                .build();
        byte[] finalBytes = RpcPacket.ADAPTER.encode(rpcPacket);

        // Simulate incoming
        transport.handler.onMessage(finalBytes);

        assertNull("Should REJECT remote event for LOCAL scope", received.get());
    }

    @Test
    public void testRemoteReceive_AuthorizedPublisher() throws Exception {
        eventBus.registerCodec(TestEvent.class, new TestEventCodec());
        eventBus.addAuthorizedPublisher(TestEvent.class, "valid-sender");

        AtomicReference<TestEvent> received = new AtomicReference<>();
        eventBus.subscribe(TestEvent.class, received::set);

        // Helper to send message from a specific publisher
        Consumer<String> sendFrom = (publisherId) -> {
            try {
                RpcPacket wireMsg = new RpcPacket.Builder()
                        .payload(ByteString.encodeUtf8("data-" + publisherId))
                        .build();
                byte[] wireBytes = RpcPacket.ADAPTER.encode(wireMsg);

                EventPacket eventPacket = new EventPacket.Builder()
                        .eventType(TestEvent.class.getName())
                        .payload(ByteString.of(wireBytes))
                        .publisherId(publisherId)
                        .timestamp(System.currentTimeMillis())
                        .build();
                byte[] eventPacketBytes = EventPacket.ADAPTER.encode(eventPacket);

                RpcPacket rpcPacket = new RpcPacket.Builder()
                        .type(RpcPacket.Type.EVENT)
                        .requestId(UUID.randomUUID().toString())
                        .payload(ByteString.of(eventPacketBytes))
                        .build();
                byte[] finalBytes = RpcPacket.ADAPTER.encode(rpcPacket);

                transport.handler.onMessage(finalBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // 1. Send from unauthorized
        sendFrom.accept("invalid-sender");
        assertNull("Should reject unauthorized publisher", received.get());

        // 2. Send from authorized
        sendFrom.accept("valid-sender");
        assertNotNull("Should accept authorized publisher", received.get());
        assertEquals("data-valid-sender", received.get().data);
    }

    @Test
    public void testLoopbackPrevention() throws Exception {
        eventBus.registerCodec(TestEvent.class, new TestEventCodec());

        AtomicReference<TestEvent> received = new AtomicReference<>();
        eventBus.subscribe(TestEvent.class, received::set);

        // Construct incoming message from SELF
        RpcPacket wireMsg = new RpcPacket.Builder()
                .payload(ByteString.encodeUtf8("incoming-data"))
                .build();
        byte[] wireBytes = RpcPacket.ADAPTER.encode(wireMsg);

        EventPacket eventPacket = new EventPacket.Builder()
                .eventType(TestEvent.class.getName())
                .payload(ByteString.of(wireBytes))
                .publisherId("test-publisher") // SAME AS SELF
                .timestamp(System.currentTimeMillis())
                .build();
        byte[] eventPacketBytes = EventPacket.ADAPTER.encode(eventPacket);

        RpcPacket rpcPacket = new RpcPacket.Builder()
                .type(RpcPacket.Type.EVENT)
                .requestId(UUID.randomUUID().toString())
                .payload(ByteString.of(eventPacketBytes))
                .build();
        byte[] finalBytes = RpcPacket.ADAPTER.encode(rpcPacket);

        // Simulate incoming
        transport.handler.onMessage(finalBytes);

        assertNull("Should ignore loopback event", received.get());
    }


    // Helpers
    static class TestEvent {
        String data;
        public TestEvent(String data) { this.data = data; }
    }

    static class TestEventCodec implements Codec<TestEvent, RpcPacket> {
        @Override
        public RpcPacket toWire(TestEvent testEvent) {
             return new RpcPacket.Builder()
                .payload(ByteString.encodeUtf8(testEvent.data))
                .build();
        }

        @Override
        public TestEvent fromWire(RpcPacket rpcPacket) {
            return new TestEvent(rpcPacket.payload.utf8());
        }

        @Override
        public ProtoAdapter<RpcPacket> getWireAdapter() {
            return RpcPacket.ADAPTER;
        }
    }

    static class MockTransport implements Transport {
        MessageHandler handler;
        byte[] lastSentMessage;

        @Override
        public void addMessageHandler(MessageHandler handler) {
            this.handler = handler;
        }

        @Override
        public void send(byte[] message) {
            this.lastSentMessage = message;
        }
    }
}
