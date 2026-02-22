package dev.verrai.integration;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.verrai.client.service.RpcClient;
import dev.verrai.client.service.UnitRegistry;
import dev.verrai.client.service.WorkerBootstrap;
import dev.verrai.client.service.transport.LocalTransport;
import dev.verrai.integration.service.dto.MyData;
import dev.verrai.integration.service.dto.Employee;
import dev.verrai.integration.service.dto.Organization;
import dev.verrai.integration.service.MyDataService;
import dev.verrai.integration.service.MyDataService_Stub;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransportTest {
    private static final Logger logger = LoggerFactory.getLogger(TransportTest.class);

    @Test
    public void testLocalTransport() {
        // 1. Setup two LocalTransports connected to each other
        LocalTransport clientSide = new LocalTransport();
        LocalTransport workerSide = new LocalTransport();

        clientSide.connect(workerSide);
        workerSide.connect(clientSide);

        // 2. Setup Client
        UnitRegistry.getInstance().registerRemote(MyDataService.class.getName(), "local-node", clientSide);
        MyDataService serviceProxy = new MyDataService_Stub();

        // 3. Setup Worker (on the same thread for this test)
        WorkerBootstrap worker = new WorkerBootstrap(workerSide);
        worker.registerDispatcher("dev.verrai.integration.service.MyDataService",
                new dev.verrai.integration.service.MyDataService_Dispatcher());

        // Setup a mock service implementation
        AtomicReference<MyData> receivedData = new AtomicReference<>();
        MyDataService implementation = new MyDataService() {
            @Override
            public dev.verrai.client.service.AsyncResult<Void> processData(MyData data) {
                logger.info("Service implementation received: {}", data.getContent());
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
        input.setId("rpc-1");
        input.setContent("Hello Pluggable Transports!");
        input.setValue(42);

        serviceProxy.processData(input);

        // 5. Verify
        assertNotNull("Worker should have received the data", receivedData.get());
        assertEquals("rpc-1", receivedData.get().getId());
        assertEquals("Hello Pluggable Transports!", receivedData.get().getContent());
        assertEquals(42, receivedData.get().getValue());

        logger.info("LocalTransport RPC test passed!");
    }
}
