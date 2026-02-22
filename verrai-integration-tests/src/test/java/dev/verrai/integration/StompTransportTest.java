package dev.verrai.integration;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.verrai.client.service.RpcClient;
import dev.verrai.client.service.UnitRegistry;
import dev.verrai.client.service.WorkerBootstrap;
import dev.verrai.client.service.transport.StompTransport;
import dev.verrai.rpc.common.transport.stomp.StompClient;
import dev.verrai.rpc.common.transport.stomp.StompMessage;
import dev.verrai.rpc.common.transport.stomp.StompSubscriptionCallback;
import dev.verrai.integration.service.dto.MyData;
import dev.verrai.integration.service.dto.Employee;
import dev.verrai.integration.service.dto.Organization;
import dev.verrai.integration.service.MyDataService;
import dev.verrai.integration.service.MyDataService_Stub;
import dev.verrai.integration.service.MyDataService_Dispatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StompTransportTest {
    private static final Logger logger = LoggerFactory.getLogger(StompTransportTest.class);

    @Test
    public void testStompTransportFlow() {
        // 1. Mock Stomp Clients (In-memory loopback)
        MockStompClient broker = new MockStompClient();

        // 2. Setup Client Side
        StompTransport clientTransport = new StompTransport(broker, "/app/rpc", "/user/queue/rpc");
        UnitRegistry.getInstance().registerRemote(MyDataService.class.getName(), "mock-node", clientTransport);
        MyDataService serviceProxy = new MyDataService_Stub();

        // 3. Setup Worker Side
        StompTransport workerTransport = new StompTransport(broker, "/user/queue/rpc", "/app/rpc");
        WorkerBootstrap worker = new WorkerBootstrap(workerTransport);
        worker.registerDispatcher("dev.verrai.integration.service.MyDataService", new MyDataService_Dispatcher());

        AtomicReference<MyData> receivedData = new AtomicReference<>();
        MyDataService implementation = new MyDataService() {
            @Override
            public dev.verrai.client.service.AsyncResult<Void> processData(MyData data) {
                logger.info("Worker implementation received: {}", data.getContent());
                receivedData.set(data);
                dev.verrai.client.service.AsyncResultImpl<Void> res = new dev.verrai.client.service.AsyncResultImpl<>();
                res.complete(null);
                return res;
            }

            @Override
            public dev.verrai.client.service.AsyncResult<Employee> getBoss(Organization org) {
                dev.verrai.client.service.AsyncResultImpl<Employee> res = new dev.verrai.client.service.AsyncResultImpl<>();
                res.complete(org.getPrimaryDepartment().getManager());
                return res;
            }
        };
        UnitRegistry.register("dev.verrai.integration.service.MyDataService", implementation);

        // 4. Invoke
        MyData input = new MyData();
        input.setId("stomp-req-1");
        input.setContent("Hello over STOMP!");

        serviceProxy.processData(input);
        // Wait for it? It's loopback, likely immediate, but technically async.
        // We checking 'receivedData' which is set in the impl.
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }

        // 5. Verify
        assertNotNull("Worker should have received the data over STOMP", receivedData.get());
        assertEquals("stomp-req-1", receivedData.get().getId());
        assertEquals("Hello over STOMP!", receivedData.get().getContent());

        logger.info("StompTransport RPC test passed!");
    }

    /**
     * A simple mock STOMP client that simulates a broker by delivering messages
     * to other clients subscribed to the same destination.
     */
    private static class MockStompClient implements StompClient {
        private final Map<String, StompSubscriptionCallback> subscriptions = new HashMap<>();

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void send(String destination, String body) {
            StompSubscriptionCallback callback = subscriptions.get(destination);
            if (callback != null) {
                callback.onMessage(new StompMessage() {
                    @Override
                    public String getBody() {
                        return body;
                    }

                    @Override
                    public Map<String, String> getHeaders() {
                        return new HashMap<>();
                    }
                });
            }
        }

        @Override
        public void subscribe(String destination, StompSubscriptionCallback callback) {
            subscriptions.put(destination, callback);
        }
    }
}
