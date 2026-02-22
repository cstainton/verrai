package uk.co.instanto.client.service;

import uk.co.instanto.client.service.dummy.TestService;
import uk.co.instanto.client.service.dummy.TestServiceImpl;
import dev.verrai.rpc.common.transport.Transport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class TearayRunnerTest {

    @Before
    public void setup() {
        UnitRegistry.getInstance().reset();
    }

    @After
    public void tearDown() {
        UnitRegistry.getInstance().reset();
    }

    @Test
    public void testServiceRegistration() {
        TearayRunner.run(MyApp.class, (registry) -> {});

        String serviceName = TestService.class.getName();
        Object registeredService = UnitRegistry.getInstance().getLocalServiceInstance(serviceName);

        assertNotNull("Service should be registered", registeredService);
        assertTrue("Service should be instance of TestServiceImpl", registeredService instanceof TestServiceImpl);
    }

    @Test
    public void testInjectionWithPreRegisteredService() {
        TestServiceImpl serviceImpl = new TestServiceImpl();
        UnitRegistry.getInstance().registerLocal(TestService.class.getName(), serviceImpl);

        TearayRunner.run(AppWithInjection.class, (registry) -> {});

        assertNotNull("App instance should be created", AppWithInjection.lastInstance);
        assertTrue("App should have started", AppWithInjection.lastInstance.started);
        assertNotNull("Service should be injected", AppWithInjection.lastInstance.injectedService);
        assertEquals("Injected service should be the pre-registered one", serviceImpl, AppWithInjection.lastInstance.injectedService);
    }

    @Test
    public void testInjectionWithCaller() {
        TestServiceImpl serviceImpl = new TestServiceImpl();
        UnitRegistry.getInstance().registerLocal(TestService.class.getName(), serviceImpl);

        TearayRunner.run(AppWithCaller.class, (registry) -> {});

        assertNotNull("App instance should be created", AppWithCaller.lastInstance);
        assertTrue("App should have started", AppWithCaller.lastInstance.started);
        assertNotNull("Caller should be injected", AppWithCaller.lastInstance.injectedCaller);
        assertEquals("Caller should wrap the service", serviceImpl, AppWithCaller.lastInstance.injectedCaller.getService());
    }

    @Test
    public void testDelayedInjection() {
        // Run without service
        TearayRunner.run(AppWithInjection.class, (registry) -> {});

        assertNotNull("App instance should be created", AppWithInjection.lastInstance);
        assertFalse("App should not start until dependency is available", AppWithInjection.lastInstance.started);

        // Register factory and remote service
        TestServiceImpl remoteStub = new TestServiceImpl();
        UnitRegistry.getInstance().registerFactory(TestService.class, (client) -> remoteStub);
        UnitRegistry.getInstance().registerRemote(TestService.class.getName(), "remote-node", mock(Transport.class));

        assertTrue("App should start after dependency becomes available", AppWithInjection.lastInstance.started);
        assertNotNull("Service should be injected", AppWithInjection.lastInstance.injectedService);
        assertEquals(remoteStub, AppWithInjection.lastInstance.injectedService);
    }

    @Test
    public void testMultipleDependencies() {
        TearayRunner.run(AppWithMultipleDeps.class, (registry) -> {});

        assertNotNull("App instance should be created", AppWithMultipleDeps.lastInstance);
        assertFalse("App should not start initially", AppWithMultipleDeps.lastInstance.started);

        // Register service factory and remote service
        TestServiceImpl stub = new TestServiceImpl();
        UnitRegistry.getInstance().registerFactory(TestService.class, (client) -> stub);
        UnitRegistry.getInstance().registerRemote(TestService.class.getName(), "remote-node", mock(Transport.class));

        assertTrue("App should start after dependencies resolved", AppWithMultipleDeps.lastInstance.started);
        assertNotNull(AppWithMultipleDeps.lastInstance.service);
        assertNotNull(AppWithMultipleDeps.lastInstance.caller);
        assertEquals(stub, AppWithMultipleDeps.lastInstance.service);
        assertEquals(stub, AppWithMultipleDeps.lastInstance.caller.getService());
    }

    // --- Dummy Applications ---

    public static class MyApp implements TearayApplication {
        public TestService service = new TestServiceImpl();

        @Override
        public void onStart() {}
    }

    public static class AppWithInjection implements TearayApplication {
        public static AppWithInjection lastInstance;

        @Inject
        public TestService injectedService;

        public boolean started = false;

        public AppWithInjection() {
            lastInstance = this;
        }

        @Override
        public void onStart() {
            started = true;
        }
    }

    public static class AppWithCaller implements TearayApplication {
        public static AppWithCaller lastInstance;

        @Inject
        public Caller<TestService> injectedCaller;

        public boolean started = false;

        public AppWithCaller() {
            lastInstance = this;
        }

        @Override
        public void onStart() {
            started = true;
        }
    }

    public static class AppWithMultipleDeps implements TearayApplication {
        public static AppWithMultipleDeps lastInstance;

        @Inject
        public TestService service;

        @Inject
        public Caller<TestService> caller;

        public boolean started = false;

        public AppWithMultipleDeps() {
            lastInstance = this;
        }

        @Override
        public void onStart() {
            started = true;
        }
    }
}
