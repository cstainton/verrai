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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.*;

public class LocalToWebWorkerTest {

    @Test
    public void testWorkerCommunication() throws Exception {
        // This test simulates a WebWorker (Client) communicating with the Main Thread/Server (Impl).
        UnitRegistry registry = UnitRegistry.getInstance();

        // 1. Setup Transports with Queues to simulate MessageChannel
        WorkerTransport clientTransport = new WorkerTransport("Worker");
        WorkerTransport serverTransport = new WorkerTransport("Server");
        clientTransport.setPeer(serverTransport);
        serverTransport.setPeer(clientTransport);

        // 2. Setup Server Side (Main Thread/JVM)
        EchoServiceImpl impl = new EchoServiceImpl("ServerNode");
        registry.register(EchoService.class, impl);
        registry.initRpcServer(serverTransport);
        registry.registerDispatcher(EchoService.class.getName(), new EchoService_Dispatcher());

        // 3. Setup Client Side (Worker)
        // In a real app, this runs in a separate thread/context.
        // We configure the registry to route "EchoService" to "ServerNode" via clientTransport.
        registry.registerRemote(EchoService.class.getName(), "ServerNode", clientTransport);

        // 4. Create Stub (In Worker)
        EchoService_Stub stub = new EchoService_Stub();

        // 5. Invoke
        Greeting request = new Greeting("Hello from Worker", "WorkerNode");
        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("[Test] Worker invoking Service...");
        stub.echo(request).thenAccept(response -> {
            System.out.println("[Test] Worker received response: " + response.getMessage());
            try {
                assertEquals("Hello from ServerNode", response.getTranslations().get("en"));
            } catch(Throwable t) {
                t.printStackTrace();
            }
            latch.countDown();
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });

        boolean success = latch.await(5, TimeUnit.SECONDS);
        clientTransport.stop();
        serverTransport.stop();

        if (!success) fail("Timeout");
    }

    // Simulates a queued transport like postMessage
    public static class WorkerTransport implements Transport {
        private final String name;
        private WorkerTransport peer;
        private final List<MessageHandler> listeners = new ArrayList<>();
        private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        private volatile boolean running = true;

        public WorkerTransport(String name) {
            this.name = name;
            // Start a thread to process incoming messages
            new Thread(() -> {
                while (running) {
                    try {
                        byte[] msg = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (msg != null) {
                            for (MessageHandler l : listeners) {
                                l.onMessage(msg);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Transport-" + name).start();
        }

        public void setPeer(WorkerTransport peer) {
            this.peer = peer;
        }

        @Override
        public void send(byte[] message) {
            if (peer != null) {
                // System.out.println("[" + name + "] Sending " + message.length + " bytes");
                peer.receive(message);
            }
        }

        @Override
        public void addMessageHandler(MessageHandler listener) {
            listeners.add(listener);
        }

        public void receive(byte[] message) {
            queue.offer(message);
        }

        public void stop() {
            running = false;
        }
    }
}
