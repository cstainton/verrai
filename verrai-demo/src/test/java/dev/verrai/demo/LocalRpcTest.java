package dev.verrai.demo;

import org.junit.Test;
import dev.verrai.client.service.UnitRegistry;
import dev.verrai.demo.service.EchoService;
import dev.verrai.demo.service.EchoServiceImpl;
import dev.verrai.demo.service.EchoService_Stub;
import dev.verrai.demo.service.EchoService_Dispatcher;
import dev.verrai.demo.service.dto.Greeting;
import dev.verrai.rpc.common.transport.Transport;
import dev.verrai.rpc.common.transport.MessageHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class LocalRpcTest {

    @Test
    public void testLoopbackRpc() throws Exception {
        UnitRegistry registry = UnitRegistry.getInstance();

        // 1. Setup Transports
        LoopbackTransport clientTransport = new LoopbackTransport();
        LoopbackTransport serverTransport = new LoopbackTransport();
        clientTransport.setPeer(serverTransport);
        serverTransport.setPeer(clientTransport);

        // 2. Setup Server Side
        // Register Implementation
        EchoServiceImpl impl = new EchoServiceImpl("ServerNode");
        registry.register(EchoService.class, impl);

        // Register Dispatcher
        registry.initRpcServer(serverTransport); // Server sends replies via serverTransport
        registry.registerDispatcher(EchoService.class.getName(), new EchoService_Dispatcher());

        // 3. Setup Client Side
        // Tell Registry that EchoService is on "RemoteNode"
        String serviceId = EchoService.class.getName();
        String remoteNodeId = "RemoteNode";

        // We manually configure the client transport for "RemoteNode"
        // UnitRegistry logic: serviceToNode -> nodeId -> nodeToTransport
        // UnitRegistry.registerRemote(serviceId, nodeId, transport) does this!

        registry.registerRemote(serviceId, remoteNodeId, clientTransport);

        // 4. Create Stub (Client)
        // Stub uses UnitRegistry.getClientForService(serviceId)
        // It should find "RemoteNode" and use clientTransport
        EchoService_Stub stub = new EchoService_Stub();

        // 5. Invoke RPC
        Greeting request = new Greeting("Hello Loopback", "ClientNode");
        CountDownLatch latch = new CountDownLatch(1);

        stub.echo(request).thenAccept(response -> {
            System.out.println("Received response: " + response.getMessage());
            try {
                // Basic assertion to verify we got something back
                if (!"Hello from ServerNode".equals(response.getTranslations().get("en"))) {
                     System.err.println("Unexpected response translation: " + response.getTranslations().get("en"));
                }
                assertEquals("Hello from ServerNode", response.getTranslations().get("en"));
            } catch (Throwable t) {
                t.printStackTrace();
            }
            latch.countDown();
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);
        if (!success) {
            fail("Test timed out");
        }
    }

    public static class LoopbackTransport implements Transport {
        private LoopbackTransport peer;
        private final List<MessageHandler> listeners = new ArrayList<>();

        public void setPeer(LoopbackTransport peer) {
            this.peer = peer;
        }

        @Override
        public void send(byte[] message) {
            if (peer != null) {
                // Run in separate thread to simulate async and avoid stack overflow or deadlock
                new Thread(() -> peer.receive(message)).start();
            }
        }

        @Override
        public void addMessageHandler(MessageHandler listener) {
            listeners.add(listener);
        }

        public void receive(byte[] message) {
            for(MessageHandler l : listeners) {
                l.onMessage(message);
            }
        }
    }
}
