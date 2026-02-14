package uk.co.instanto.integration;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.instanto.client.service.AsyncResult;
import uk.co.instanto.client.service.RpcClient;
import uk.co.instanto.client.service.UnitRegistry;
import uk.co.instanto.client.service.WorkerBootstrap;
import uk.co.instanto.integration.transport.SimulatedWebWorkerTransport;
import dev.verrai.rpc.common.transport.stomp.StompClient;
import dev.verrai.rpc.common.transport.stomp.StompMessage;
import dev.verrai.rpc.common.transport.stomp.StompSubscriptionCallback;
import uk.co.instanto.client.service.transport.StompTransport;
import uk.co.instanto.integration.service.MyDataService;
import uk.co.instanto.integration.service.MyDataService_Stub;
import uk.co.instanto.integration.service.MyDataService_Dispatcher;
import uk.co.instanto.integration.service.dto.MyData;
import uk.co.instanto.integration.service.dto.Employee;
import uk.co.instanto.integration.service.dto.Organization;
import uk.co.instanto.integration.service.AuthenticationService;
import uk.co.instanto.integration.service.AuthenticationService_Stub;
import uk.co.instanto.integration.service.AuthenticationService_Dispatcher;
import uk.co.instanto.integration.service.dto.LogonRequest;
import uk.co.instanto.client.service.dto.LogonResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test verifying the architecture of a Service running in a
 * WebWorker
 * communicating with a Service in the JVM (Server), mediated by the Main
 * Thread.
 */
public class WebWorkerIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(WebWorkerIntegrationTest.class);

    @Test
    public void testWorkerToJvmRpc() throws InterruptedException {
        // --- 1. Infrastructure Setup ---

        // Mock Broker (The Network)
        MockStompClient broker = new MockStompClient();

        // Server Side (JVM)
        // Uses StompTransport directly connected to Broker
        StompTransport serverTransport = new StompTransport(broker, "/user/queue/rpc", "/app/rpc");
        WorkerBootstrap serverBootstrap = new WorkerBootstrap(serverTransport);
        serverBootstrap.registerDispatcher("uk.co.instanto.integration.service.MyDataService",
                new MyDataService_Dispatcher());
        serverBootstrap.registerDispatcher("uk.co.instanto.integration.service.AuthenticationService",
                new AuthenticationService_Dispatcher());

        // Register Implementation
        UnitRegistry.register("uk.co.instanto.integration.service.MyDataService", new MyDataService() {
            @Override
            public AsyncResult<Void> processData(MyData data) {
                logger.info("JVM Server received: " + data.getContent());
                uk.co.instanto.client.service.AsyncResultImpl<Void> res = new uk.co.instanto.client.service.AsyncResultImpl<>();
                res.complete(null);
                return res;
            }

            @Override
            public AsyncResult<Employee> getBoss(Organization org) {
                return null;
            }
        });

        UnitRegistry.register("uk.co.instanto.integration.service.AuthenticationService", new AuthenticationService() {
            @Override
            public AsyncResult<LogonResponse> login(LogonRequest request) {
                logger.info("JVM Server received login for: " + request.getUsername());
                uk.co.instanto.client.service.AsyncResultImpl<LogonResponse> res = new uk.co.instanto.client.service.AsyncResultImpl<>();
                res.complete(new LogonResponse("signed-jwt-token-" + request.getUsername(), "stomp-address"));
                return res;
            }
        });

        // --- 2. Client Setup (Simulated WebWorker) ---

        // The transport inside the Worker
        SimulatedWebWorkerTransport workerTransport = new SimulatedWebWorkerTransport();

        // Run Worker Logic in a separate thread
        AtomicReference<Throwable> workerError = new AtomicReference<>();
        CountDownLatch doneLatch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                logger.info("[Worker] Starting...");
                UnitRegistry.getInstance().registerRemote(MyDataService.class.getName(), "simulated-worker-node", workerTransport);
                UnitRegistry.getInstance().registerRemote(AuthenticationService.class.getName(), "simulated-worker-node", workerTransport);

                MyDataService workerStub = new MyDataService_Stub();
                AuthenticationService authStub = new AuthenticationService_Stub();

                // 1. Existing Test
                MyData req = new MyData();
                req.setId("worker-1");
                req.setContent("Hello from Worker");

                logger.info("[Worker] Calling MyDataService...");
                workerStub.processData(req);
                logger.info("[Worker] MyDataService Call returned.");

                // 2. New Authentication Test
                LogonRequest loginReq = new LogonRequest("user123", "secret");
                logger.info("[Worker] Calling AuthenticationService.login...");
                AsyncResult<LogonResponse> loginResult = authStub.login(loginReq);

                // Block/Wait for result (simulated by checking if we can attach callback)
                final CountDownLatch loginLatch = new CountDownLatch(1);
                loginResult.thenAccept(response -> {
                    try {
                        logger.info("[Worker] Login Success! Token: " + response.getJwtToken());
                        if (!"signed-jwt-token-user123".equals(response.getJwtToken())) {
                             throw new RuntimeException("Unexpected token: " + response.getJwtToken());
                        }
                        loginLatch.countDown();
                    } catch (Exception e) {
                        workerError.set(e);
                        loginLatch.countDown();
                    }
                });

                if (!loginLatch.await(2000, TimeUnit.MILLISECONDS)) {
                     throw new RuntimeException("Login timed out");
                }

                logger.info("[Worker] All calls complete.");
                doneLatch.countDown();

            } catch (Exception e) {
                e.printStackTrace();
                workerError.set(e);
                doneLatch.countDown();
            }
        }, "Worker-Thread").start();

        // --- 3. Main Thread Bridge (The Browser Main Thread) ---
        // This simulates the JS code that bridges postMessage <-> WebSocket/STOMP

        MockStompClient bridgeStompClient = new MockStompClient();
        // In reality, Main Thread connects to same broker as Server.
        // Note: MockStompClient is currently isolated instances. We need them to share
        // state or be the same instance.
        // Let's use the SAME broker instance for simulated network.

        StompClient bridgeWithNetwork = new ForwardingStompClient(broker);
        // Actually, StompTransport takes a StompClient.
        // We need a custom logic to bridge.

        // Subscribe Bridge to Server Responses (/user/queue/rpc for the client)
        broker.subscribe("/user/queue/rpc", message -> {
            // Received response from Server via STOMP
            logger.info("[Bridge] Received STOMP message from Server. Forwarding to Worker.");

            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(message.getBody());
                workerTransport.injectIncoming(decoded);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Loop to forward Worker -> Server
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < 10000) { // Increased timeout for multiple calls
            byte[] msg = workerTransport.pollOutgoing(100, TimeUnit.MILLISECONDS);
            if (msg != null) {
                logger.info("[Bridge] Received bytes from Worker. Wrapping in STOMP and sending to Server.");
                // Encode bytes -> Base64
                String base64 = java.util.Base64.getEncoder().encodeToString(msg);
                // Send to Server destination
                broker.send("/app/rpc", base64);
            }
            if (doneLatch.getCount() == 0)
                break;
        }

        if (workerError.get() != null) {
            throw new RuntimeException("Worker failed", workerError.get());
        }

        if (doneLatch.getCount() > 0) {
             throw new RuntimeException("Test Timed Out - Worker did not finish");
        }

        // Assert we actually verified the loop
        // We can't easily verify the server *received* it unless we mock the server
        // impl with a latch.
        // Let's update the Server Implementation to countdown too.
    }

    // --- Helpers ---

    // Simplified Mock Client sharing subscriptions via static or reference?
    // The MockStompClient in StompTransportTest was isolated.
    // We need a "Network" (Broker) that both connect to.

    private static class MockStompClient implements StompClient {
        // Shared state for this test instance?
        // Ideally pass a 'Router' object.
        private final Map<String, StompSubscriptionCallback> subscriptions = new HashMap<>();
        // In a real broker, many clients connect.
        // Here, we act as the Singleton Broker.
        // But the StompTransport needs a StompClient instance to call.
        // If we pass the SAME MockStompClient instance to both Server side and Bridge
        // side,
        // calling 'send' triggers 'subscriptions' on that instance.
        // That works for a simple in-memory bus!

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void send(String destination, String body) {
            logger.info("[Network] SEND dest={} bodyLen={}", destination, body.length());
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
            } else {
                logger.warn("[Network] No subscribers for {}", destination);
            }
        }

        @Override
        public void subscribe(String destination, StompSubscriptionCallback callback) {
            logger.info("[Network] SUBSCRIBE dest={}", destination);
            subscriptions.put(destination, callback);
        }
    }

    private static class ForwardingStompClient implements StompClient {
        private final MockStompClient delegate;

        public ForwardingStompClient(MockStompClient delegate) {
            this.delegate = delegate;
        }

        public boolean isConnected() {
            return true;
        }

        public void send(String d, String b) {
            delegate.send(d, b);
        }

        public void subscribe(String d, StompSubscriptionCallback c) {
            delegate.subscribe(d, c);
        }
    }

}
