package dev.verrai.client.service;

import dev.verrai.rpc.common.transport.MessageHandler;
import dev.verrai.rpc.common.transport.Transport;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class UnitRegistryTest {

    private UnitRegistry registry;

    @Before
    public void setUp() {
        registry = new UnitRegistry();
    }

    @Test
    public void testSingletonAndInstance() {
        UnitRegistry instance1 = UnitRegistry.getInstance();
        UnitRegistry instance2 = UnitRegistry.getInstance();
        assertSame(instance1, instance2);
        assertNotSame(instance1, registry);
    }

    @Test
    public void testRegisterAndGetLocalService() {
        String serviceName = "LocalService";
        Object serviceInstance = new Object();
        registry.registerLocal(serviceName, serviceInstance);

        Object retrieved = registry.getLocalServiceInstance(serviceName);
        assertSame(serviceInstance, retrieved);

        // Also verify generic getService works for local
        // Using a dummy class for testing since getService uses class name
        class TestService {}
        TestService ts = new TestService();
        registry.registerLocal(TestService.class.getName(), ts);
        TestService retrievedTs = registry.getService(TestService.class);
        assertSame(ts, retrievedTs);
    }

    @Test
    public void testRegisterFactory() {
        // Just verify no exception is thrown
        registry.registerFactory(String.class, client -> "FactoryCreatedString");
    }

    @Test
    public void testRegisterRemoteAndGetTransport() {
        String serviceId = "RemoteService";
        String nodeId = "NodeA";
        MockTransport transport = new MockTransport();

        registry.registerRemote(serviceId, nodeId, transport);

        Transport retrievedTransport = registry.getTransportForServiceInstance(serviceId);
        assertSame(transport, retrievedTransport);
    }

    @Test
    public void testGetService_Remote() {
        // Setup
        String nodeId = "NodeB";
        MockTransport transport = new MockTransport();

        // Register transport for node (implicitly done via registerRemote)
        // However, registerRemote takes serviceId, nodeId, transport.

        // We need a service interface and factory
        class RemoteServiceStub {}
        registry.registerFactory(RemoteServiceStub.class, client -> new RemoteServiceStub());

        // Register remote service
        String serviceId = RemoteServiceStub.class.getName();
        registry.registerRemote(serviceId, nodeId, transport);

        // Get service
        RemoteServiceStub stub = registry.getService(RemoteServiceStub.class);
        assertNotNull(stub);
        assertTrue(stub instanceof RemoteServiceStub);
    }

    @Test
    public void testAwaitService_AlreadyRegistered() {
        String serviceName = "ExistingService";
        Object serviceInstance = new Object();
        registry.registerLocal(serviceName, serviceInstance);

        AtomicReference<Object> callbackResult = new AtomicReference<>();
        // We need a class that matches the service name for type safety in awaitService
        // But awaitService uses Class<T>, so let's use Object.class and manually register with Object.class.getName()
        // Wait, registerLocal takes a name.

        // Let's use a real class
        class MyService {}
        MyService myService = new MyService();
        registry.registerLocal(MyService.class.getName(), myService);

        registry.awaitService(MyService.class, callbackResult::set);

        assertSame(myService, callbackResult.get());
    }

    @Test
    public void testAwaitService_FutureRegistration() {
        class FutureService {}
        AtomicReference<FutureService> callbackResult = new AtomicReference<>();

        registry.awaitService(FutureService.class, callbackResult::set);
        assertNull(callbackResult.get());

        FutureService service = new FutureService();
        // Mimic remote registration which triggers callbacks
        // Note: registerLocal does NOT trigger callbacks in the current implementation of UnitRegistry?
        // Let's check the code.
        // Code: registerRemote checks pending callbacks. registerLocal does NOT.
        // This seems like a potential bug or design choice. The test should respect current behavior.

        // So we must use registerRemote to trigger callback.
        // registerRemote needs a transport.
        MockTransport transport = new MockTransport();
        // It also tries to create a stub using a factory.
        registry.registerFactory(FutureService.class, client -> service);

        registry.registerRemote(FutureService.class.getName(), "NodeC", transport);

        assertNotNull(callbackResult.get());
        assertSame(service, callbackResult.get());
    }

    @Test
    public void testCleanupStaleServices() throws InterruptedException {
        String serviceId = "StaleService";
        String nodeId = "StaleNode";
        MockTransport transport = new MockTransport();

        registry.registerRemote(serviceId, nodeId, transport);

        // Verify it's there
        assertNotNull(registry.getTransportForServiceInstance(serviceId));

        // Wait or simulate time passing. The method uses System.currentTimeMillis().
        // We can't easily mock time in UnitRegistry without refactoring.
        // So we will set a very short timeout.

        // Sleep for a bit to ensure diff > 10ms
        Thread.sleep(50);

        registry.cleanupStaleServices(10); // 10ms timeout

        // Verify it's gone
        assertNull(registry.getTransportForServiceInstance(serviceId));
    }

    @Test
    public void testReset() {
        registry.registerLocal("SomeService", new Object());
        assertFalse(registry.getLocalServiceIdsInstance().isEmpty());

        registry.reset();

        assertTrue(registry.getLocalServiceIdsInstance().isEmpty());
    }

    // Helpers
    static class MockTransport implements Transport {
        @Override
        public void addMessageHandler(MessageHandler handler) {}

        @Override
        public void send(byte[] message) {}
    }
}
